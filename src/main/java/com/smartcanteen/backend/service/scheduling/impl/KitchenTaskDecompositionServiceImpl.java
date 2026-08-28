package com.smartcanteen.backend.service.scheduling.impl;

import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.FoodCategory;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.ItemType;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderItem;
import com.smartcanteen.backend.service.scheduling.KitchenTaskDecompositionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class KitchenTaskDecompositionServiceImpl
        implements KitchenTaskDecompositionService {

    @Override
    public List<SchedulingTask> decompose(Order order) {

        List<SchedulingTask> tasks = new ArrayList<>();

        if (order == null || order.getOrderItems() == null) {
            return tasks;
        }

        for (OrderItem orderItem : order.getOrderItems()) {

            if (orderItem == null) {
                continue;
            }

            FoodItem foodItem = orderItem.getFoodItem();

            /*
             * Only cooked/prepared food requires a kitchen
             * preparation scheduling task.
             *
             * READY_MADE items do not require preparation scheduling.
             */
            if (foodItem == null
                    || foodItem.getItemType() != ItemType.COOKED) {
                continue;
            }

            Integer quantity = orderItem.getQuantity();

            /*
             * Invalid quantity must never create a negative
             * or zero/invalid preparation task.
             */
            if (quantity == null || quantity <= 0) {
                continue;
            }

            /*
             * durationMinutes represents preparation workload,
             * preserving the existing backend semantics:
             *
             * prepTimeMinutes × quantity
             *
             * Use long during multiplication to prevent integer
             * overflow before converting back to int.
             */
            long duration = (long) foodItem.getPrepTimeMinutes() * quantity;

            if (duration > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Preparation duration is too large for scheduling"
                );
            }

            int durationMinutes = (int) duration;

            KitchenResourceType resource = resolveResource(foodItem);

            tasks.add(new SchedulingTask(
                    order.getId(),
                    orderItem.getId(),
                    foodItem.getId(),
                    quantity,
                    resource,
                    durationMinutes
            ));
        }

        return tasks;
    }

    /**
     * Resolves the operational kitchen resource required
     * by a cooked food item.
     *
     * Explicit FoodItem.requiredResource takes precedence.
     *
     * For legacy food items where requiredResource is null,
     * a deterministic fallback is used.
     */
    private KitchenResourceType resolveResource(FoodItem foodItem) {

        if (foodItem.getRequiredResource() != null) {
            return foodItem.getRequiredResource();
        }

        if (foodItem.getCategory() == FoodCategory.BEVERAGE) {
            return KitchenResourceType.BEVERAGE;
        }

        return KitchenResourceType.PREPARATION;
    }
}