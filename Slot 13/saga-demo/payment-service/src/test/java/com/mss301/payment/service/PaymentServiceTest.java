package com.mss301.payment.service;

import com.mss301.payment.dto.PaymentRequest;
import com.mss301.payment.entity.*;
import com.mss301.payment.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceTest {
    private final WalletRepository wallets=mock(WalletRepository.class);
    private final PaymentRepository payments=mock(PaymentRepository.class);
    private final PaymentService service=new PaymentService(wallets,payments);

    @Test void duplicatePayDoesNotChargeAgain() {
        Payment p=new Payment();p.setOrderId(1L);p.setStatus(PaymentStatus.PAID);
        when(payments.findById(1L)).thenReturn(Optional.of(p));
        assertSame(p,service.pay(new PaymentRequest(1L,"C1",2000)));
        verifyNoInteractions(wallets);verify(payments,never()).save(any());
    }
    @Test void insufficientBalanceReturns402WithoutChangingWallet() {
        Wallet w=new Wallet("C2",100);when(wallets.findById("C2")).thenReturn(Optional.of(w));
        var error=assertThrows(ResponseStatusException.class,()->service.pay(new PaymentRequest(1L,"C2",1000)));
        assertEquals(HttpStatus.PAYMENT_REQUIRED,error.getStatusCode());assertTrue(error.getReason().startsWith("Số dư không đủ"));
        assertEquals(100,w.getBalance());verify(payments,never()).save(any());
    }
    @Test void unknownWalletReturns404() {
        var error=assertThrows(ResponseStatusException.class,()->service.pay(new PaymentRequest(1L,"C9",20)));
        assertEquals(HttpStatus.NOT_FOUND,error.getStatusCode());
    }
    @Test void refundReturnsMoneyOnlyOnce() {
        Wallet w=new Wallet("C1",3000);Payment p=new Payment();p.setOrderId(1L);p.setCustomerId("C1");p.setAmount(2000);p.setStatus(PaymentStatus.PAID);
        when(payments.findById(1L)).thenReturn(Optional.of(p));when(wallets.findById("C1")).thenReturn(Optional.of(w));
        service.refund(1L);service.refund(1L);
        assertEquals(5000,w.getBalance());assertEquals(PaymentStatus.REFUNDED,p.getStatus());
        verify(wallets,times(1)).findById("C1");
    }
    @Test void refundWithoutPaymentDoesNotCreditWallet() {
        service.refund(42L);verifyNoInteractions(wallets);
    }
}
