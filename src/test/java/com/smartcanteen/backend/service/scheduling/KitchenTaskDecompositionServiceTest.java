package com.smartcanteen.backend.service.scheduling;

import com.smartcanteen.backend.dto.scheduling.SchedulingTask;
import com.smartcanteen.backend.entity.FoodCategory;
import com.smartcanteen.backend.entity.FoodItem;
import com.smartcanteen.backend.entity.ItemType;
import com.smartcanteen.backend.entity.KitchenResourceType;
import com.smartcanteen.backend.entity.Order;
import com.smartcanteen.backend.entity.OrderItem;
import com.smartcanteen.backend.service.scheduling.impl.KitchenTaskDecompositionServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KitchenTaskDecompositionServiceTest {

    private final KitchenTaskDecompositionService service =
            new KitchenTaskDecompositionServiceImpl();

    @Test
    void singleCookedOrderItemCreatesOneTask() {

        Order order = orderWithItems(
                orderItem(
                        cookedFood(
                                FoodCategory.MAIN,
                                8,
                                KitchenResourceType.GRILL
                        ),
                        1
                )
        );

        List<SchedulingTask> tasks = service.decompose(order);

        assertEquals(1, tasks.size());
        assertEquals(1, tasks.get(0).quantity());
        assertEquals(8, tasks.get(0).durationMinutes());
        assertEquals(
                KitchenResourceType.GRILL,
                tasks.get(0).requiredResource()
        );
    }

    @Test
    void multipleCookedOrderItemsCreateMultipleTasks() {

        Order order = orderWithItems(
                orderItem(
                        cookedFood(
                                FoodCategory.MAIN,
                                8,
                                KitchenResourceType.GRILL
                        ),
                        1
                ),
                orderItem(
                        cookedFood(
                                FoodCategory.SNACK,
                                4,
                                KitchenResourceType.FRYER
                        ),
                        1
                )
        );

        List<SchedulingTask> tasks = service.decompose(order);

        assertEquals(2, tasks.size());

        assertEquals(
                KitchenResourceType.GRILL,
                tasks.get(0).requiredResource()
        );

        assertEquals(
                KitchenResourceType.FRYER,
                tasks.get(1).requiredResource()
        );
    }

    @Test
    void quantityGreaterThanOneMultipliesDuration() {

        Order order = orderWithItems(
                orderItem(
                        cookedFood(
                                FoodCategory.MAIN,
                                5,
                                KitchenResourceType.PREPARATION
                        ),
                        3
                )
        );

        List<SchedulingTask> tasks = service.decompose(order);

        assertEquals(1, tasks.size());
        assertEquals(3, tasks.get(0).quantity());
        assertEquals(15, tasks.get(0).durationMinutes());
    }

    @Test
    void readyMadeItemCreatesNoSchedulingTask() {

        Order order = orderWithItems(
                orderItem(
                        readyMadeFood(
                                FoodCategory.SNACK,
                                0
                        ),
                        1
                )
        );

        List<SchedulingTask> tasks = service.decompose(order);

        assertTrue(tasks.isEmpty());
    }

    @Test
    void mixedOrderCreatesTaskOnlyForCookedItem() {

        Order order = orderWithItems(
                orderItem(
                        readyMadeFood(
                                FoodCategory.SNACK,
                                0
                        ),
                        1
                ),
                orderItem(
                        cookedFood(
                                FoodCategory.MAIN,
                                7,
                                KitchenResourceType.GRILL
                        ),
                        1
                )
        );

        List<SchedulingTask> tasks = service.decompose(order);

        assertEquals(1, tasks.size());
        assertEquals(7, tasks.get(0).durationMinutes());
        assertEquals(
                KitchenResourceType.GRILL,
                tasks.get(0).requiredResource()
        );
    }

    @Test
    void explicitGrillResourceIsUsed() {

        assertExplicitResource(KitchenResourceType.GRILL);
    }

    @Test
    void explicitFryerResourceIsUsed() {

        assertExplicitResource(KitchenResourceType.FRYER);
    }

    @Test
    void explicitBeverageResourceIsUsed() {

        assertExplicitResource(KitchenResourceType.BEVERAGE);
    }

    @Test
    void explicitPreparationResourceIsUsed() {

        assertExplicitResource(KitchenResourceType.PREPARATION);
    }

    @Test
    void nullRequiredResourceWithBeverageCategoryFallsBackToBeverage() {

        Order order = orderWithItems(
                orderItem(
                        cookedFood(
                                FoodCategory.BEVERAGE,
                                2,
                                null
                        ),
                        1
                )
        );

        List<SchedulingTask> tasks = service.decompose(order);

        assertEquals(1, tasks.size());
        assertEquals(
                KitchenResourceType.BEVERAGE,
                tasks.get(0).requiredResource()
        );
    }

    @Test
    void nullRequiredResourceWithNonBeverageCookedItemFallsBackToPreparation() {

        Order order = orderWithItems(
                orderItem(
                        cookedFood(
                                FoodCategory.MAIN,
                                6,
                                null
                        ),
                        1
                )
        );

        List<SchedulingTask> tasks = service.decompose(order);

        assertEquals(1, tasks.size());
        assertEquals(
                KitchenResourceType.PREPARATION,
                tasks.get(0).requiredResource()
        );
    }

    @Test
    void nullFoodItemDoesNotThrowAndCreatesNoTask() {

        OrderItem item = new OrderItem();
        item.setQuantity(1);

        Order order = orderWithItems(item);

        List<SchedulingTask> tasks = service.decompose(order);

        assertTrue(tasks.isEmpty());
    }

    @Test
    void invalidQuantityCreatesNoTask() {

        Order order = orderWithItems(
                orderItem(
                        cookedFood(
                                FoodCategory.MAIN,
                                5,
                                KitchenResourceType.GRILL
                        ),
                        0
                ),
                orderItem(
                        cookedFood(
                                FoodCategory.MAIN,
                                5,
                                KitchenResourceType.GRILL
                        ),
                        -1
                )
        );

        List<SchedulingTask> tasks = service.decompose(order);

        assertTrue(tasks.isEmpty());
    }

    @Test
    void nullOrderReturnsEmptyTaskList() {

        List<SchedulingTask> tasks = service.decompose(null);

        assertTrue(tasks.isEmpty());
    }

    @Test
    void orderWithNullOrderItemsReturnsEmptyTaskList() {

        Order order = new Order();
        order.setOrderItems(null);

        List<SchedulingTask> tasks = service.decompose(order);

        assertTrue(tasks.isEmpty());
    }

    private void assertExplicitResource(
            KitchenResourceType resourceType) {

        Order order = orderWithItems(
                orderItem(
                        cookedFood(
                                FoodCategory.MAIN,
                                5,
                                resourceType
                        ),
                        1
                )
        );

        List<SchedulingTask> tasks = service.decompose(order);

        assertEquals(1, tasks.size());
        assertEquals(
                resourceType,
                tasks.get(0).requiredResource()
        );
    }

    private Order orderWithItems(OrderItem... items) {

        Order order = new Order();

        for (OrderItem item : items) {
            item.setOrder(order);
        }

        order.setOrderItems(List.of(items));

        return order;
    }

    private OrderItem orderItem(
            FoodItem foodItem,
            Integer quantity) {

        OrderItem orderItem = new OrderItem();

        orderItem.setFoodItem(foodItem);
        orderItem.setQuantity(quantity);

        return orderItem;
    }

    private FoodItem cookedFood(
            FoodCategory category,
            int prepTimeMinutes,
            KitchenResourceType resourceType) {

        FoodItem foodItem = new FoodItem();

        foodItem.setName("Test Cooked Item");
        foodItem.setCategory(category);
        foodItem.setItemType(ItemType.COOKED);
        foodItem.setPrepTimeMinutes(prepTimeMinutes);
        foodItem.setRequiredResource(resourceType);

        return foodItem;
    }

    private FoodItem readyMadeFood(
            FoodCategory category,
            int prepTimeMinutes) {

        FoodItem foodItem = new FoodItem();

        foodItem.setName("Test Ready Made Item");
        foodItem.setCategory(category);
        foodItem.setItemType(ItemType.READY_MADE);
        foodItem.setPrepTimeMinutes(prepTimeMinutes);

        return foodItem;
    }
}