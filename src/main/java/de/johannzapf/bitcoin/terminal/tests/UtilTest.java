package de.johannzapf.bitcoin.terminal.tests;


import de.johannzapf.bitcoin.terminal.exception.PaymentFailedException;
import de.johannzapf.bitcoin.terminal.service.AddressService;

public class UtilTest {

    public static void main(String[] args) throws PaymentFailedException {

        AddressService.getAddressInfo("n3JE86hQr3hhjH2cXBHbVCYUzrmQSePKB6");
    }
}
