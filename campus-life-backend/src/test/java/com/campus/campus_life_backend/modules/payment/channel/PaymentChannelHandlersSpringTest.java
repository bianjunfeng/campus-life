package com.campus.campus_life_backend.modules.payment.channel;

import com.campus.campus_life_backend.modules.payment.service.PaymentCallbackService;
import com.campus.campus_life_backend.modules.payment.service.PaymentOrderDomainService;
import com.campus.campus_life_backend.modules.user.mapper.UserWalletMapper;
import com.campus.campus_life_backend.modules.user.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = PaymentChannelHandlersSpringTest.ChannelHandlersTestApplication.class)
@ActiveProfiles("test")
class PaymentChannelHandlersSpringTest {

    @Autowired
    private List<PaymentChannelHandler> channelHandlers;

    @Test
    void contextContainsThreeHandlersWithExpectedChannelCodes() {
        assertEquals(3, channelHandlers.size());
        Set<String> codes = channelHandlers.stream()
                .map(PaymentChannelHandler::channelCode)
                .collect(Collectors.toSet());
        assertEquals(Set.of("alipay", "wechat", "wallet"), codes);
    }

    @SpringBootConfiguration
    @ComponentScan(basePackageClasses = {
            AlipayChannelHandler.class,
            WechatChannelHandler.class,
            WalletChannelHandler.class,
            PaymentChannelRegistry.class
    })
    static class ChannelHandlersTestApplication {

        @Bean
        PaymentCallbackService paymentCallbackService() {
            return mock(PaymentCallbackService.class);
        }

        @Bean
        PaymentOrderDomainService paymentOrderDomainService() {
            return mock(PaymentOrderDomainService.class);
        }

        @Bean
        UserWalletMapper userWalletMapper() {
            return mock(UserWalletMapper.class);
        }

        @Bean
        WalletTransactionMapper walletTransactionMapper() {
            return mock(WalletTransactionMapper.class);
        }
    }
}
