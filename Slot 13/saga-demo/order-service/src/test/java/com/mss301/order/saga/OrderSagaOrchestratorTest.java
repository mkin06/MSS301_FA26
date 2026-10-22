package com.mss301.order.saga;

import com.mss301.order.client.*;
import com.mss301.order.dto.CreateOrderRequest;
import com.mss301.order.entity.*;
import com.mss301.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderSagaOrchestratorTest {
    private final OrderService orders = mock(OrderService.class);
    private final InventoryClient inventory = mock(InventoryClient.class);
    private final PaymentClient payments = mock(PaymentClient.class);
    private final OrderSagaOrchestrator saga = new OrderSagaOrchestrator(orders, inventory, payments);
    private final CreateOrderRequest request = new CreateOrderRequest("C1", "P1", 2, 2000);
    private final Order order = new Order();

    @BeforeEach void setUp() {
        order.setId(7L);
        when(orders.createPendingOrder(request)).thenReturn(order);
        when(orders.findById(7L)).thenReturn(order);
    }
    @Test void successfulSagaReservesPaysAndConfirmsInOrder() {
        assertSame(order, saga.placeOrder(request));
        var sequence = inOrder(inventory, payments, orders);
        sequence.verify(orders).createPendingOrder(request);
        sequence.verify(orders).updateSagaStatus(7L, SagaStatus.EXECUTING);
        sequence.verify(inventory).reserve(7L, "P1", 2);
        sequence.verify(payments).pay(7L, "C1", 2000);
        sequence.verify(orders).confirmOrder(7L);
        verify(inventory, never()).release(anyLong());
        verify(payments, never()).refund(anyLong());
    }
    @Test void inventoryFailureNeverChargesOrReleases() {
        doThrow(new RestClientException("no stock")).when(inventory).reserve(7L,"P1",2);
        saga.placeOrder(request);
        verifyNoInteractions(payments);
        verify(inventory, never()).release(anyLong());
        verify(orders).markFailed(eq(7L), contains("no stock"));
        verify(orders).cancelOrder(7L);
    }
    @Test void failedPaymentReleasesInventoryWithoutRefunding() {
        doThrow(new RestClientException("no balance")).when(payments).pay(7L,"C1",2000);
        saga.placeOrder(request);
        var sequence=inOrder(orders,inventory);
        sequence.verify(orders).markFailed(eq(7L),anyString());
        sequence.verify(orders).updateSagaStatus(7L,SagaStatus.COMPENSATING);
        sequence.verify(inventory).release(7L);
        sequence.verify(orders).cancelOrder(7L);
        verify(payments,never()).refund(anyLong());
    }
    @Test void failedLocalConfirmRefundsThenReleasesThenCancels() {
        doThrow(new IllegalStateException("confirm failed")).when(orders).confirmOrder(7L);
        saga.placeOrder(request);
        var sequence=inOrder(payments,inventory,orders);
        sequence.verify(payments).refund(7L);
        sequence.verify(inventory).release(7L);
        sequence.verify(orders).cancelOrder(7L);
    }
    @Test void retriesCompensationAtMostThreeTimes() {
        doThrow(new RestClientException("pay down")).when(payments).pay(7L,"C1",2000);
        doThrow(new RestClientException("release down")).when(inventory).release(7L);
        saga.placeOrder(request);
        verify(inventory,times(3)).release(7L);
        verify(orders,times(3)).log(eq(7L),eq("COMPENSATION"),eq("FAILED"),anyString());
        verify(orders).updateSagaStatus(7L,SagaStatus.COMPENSATING);
        verify(orders,never()).cancelOrder(anyLong());
    }
    @Test void transientCompensationFailureRecoversOnThirdAttempt() {
        doThrow(new RestClientException("pay down")).when(payments).pay(7L,"C1",2000);
        doThrow(new RestClientException("down")).doThrow(new RestClientException("down")).doNothing().when(inventory).release(7L);
        saga.placeOrder(request);
        verify(inventory,times(3)).release(7L);
        verify(orders).log(7L,"COMPENSATION","SUCCESS","Lần thử 3");
        verify(orders).cancelOrder(7L);
    }
    @Test void exhaustedRefundStillAttemptsOtherCompensation() {
        doThrow(new IllegalStateException("confirm failed")).when(orders).confirmOrder(7L);
        doThrow(new RestClientException("refund down")).when(payments).refund(7L);
        saga.placeOrder(request);
        verify(payments,times(3)).refund(7L);
        verify(inventory).release(7L);
        verify(orders,never()).cancelOrder(anyLong());
    }
    @Test void orchestratorDoesNotDeclareADistributedTransaction() throws Exception {
        assertNull(OrderSagaOrchestrator.class.getAnnotation(Transactional.class));
        assertNull(OrderSagaOrchestrator.class.getMethod("placeOrder",CreateOrderRequest.class).getAnnotation(Transactional.class));
    }
}
