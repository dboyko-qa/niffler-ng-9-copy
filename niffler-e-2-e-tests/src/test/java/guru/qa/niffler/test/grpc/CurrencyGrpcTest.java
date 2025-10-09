package guru.qa.niffler.test.grpc;

import com.google.protobuf.Empty;
import guru.qa.niffler.grpc.*;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CurrencyGrpcTest extends BaseGrpcTest {

  Map<CurrencyValues, Double> currencyRates = Map.of(
          CurrencyValues.RUB, 	0.015,
          CurrencyValues.KZT,	0.0021,
          CurrencyValues.EUR,	1.08,
          CurrencyValues.USD, 	1.0
  );


  @Test
  void allCurrenciesShouldReturned() {
    final CurrencyResponse response = blockingStub.getAllCurrencies(Empty.getDefaultInstance());
    final List<Currency> allCurrenciesList = response.getAllCurrenciesList();
    List<String> actualValues = allCurrenciesList.stream()
            .map(c -> c.getCurrency().getValueDescriptor().getName()).toList();
    List<String> expectedValues = Arrays.stream(guru.qa.niffler.model.CurrencyValues.values())
            .map(c -> c.name()).toList();
    Assertions.assertEquals(expectedValues.size(), allCurrenciesList.size());
    Assertions.assertTrue(actualValues.containsAll(expectedValues));
    Assertions.assertTrue(allCurrenciesList.stream().map(c -> c.getCurrencyRate()).allMatch(c -> c > 0));
  }


  @Test
  void testCalculateRateEqualCurrency() {
    Double amount = 3.0;
    CalculateResponse response = blockingStub.calculateRate(CalculateRequest.newBuilder()
                    .setSpendCurrency(CurrencyValues.USD)
                    .setDesiredCurrency(CurrencyValues.USD)
                    .setAmount(amount)
            .build());
    Double resultRate = response.getCalculatedAmount();
    Assertions.assertEquals(amount, resultRate, 0.01);
  }

  @Test
  void testCalculateRateEqualCurrencyNotUSD() {
    Double amount = 3.0;
    CalculateResponse response = blockingStub.calculateRate(CalculateRequest.newBuilder()
            .setSpendCurrency(CurrencyValues.RUB)
            .setDesiredCurrency(CurrencyValues.RUB)
            .setAmount(amount)
            .build());
    Double resultRate = response.getCalculatedAmount();
    Assertions.assertEquals(amount, resultRate, 0.01);
  }

  @Test
  void testCalculateRateStraightCourse() {
    Double sourceAmount = 30000.0;
    Double resultAmount = sourceAmount * currencyRates.get(CurrencyValues.RUB);
    CalculateResponse response = blockingStub.calculateRate(CalculateRequest.newBuilder()
                    .setSpendCurrency(CurrencyValues.RUB)
                    .setDesiredCurrency(CurrencyValues.USD)
                    .setAmount(sourceAmount)
            .build());
    Double resultRate = response.getCalculatedAmount();
    Assertions.assertEquals(resultAmount, resultRate, 0.01);
  }

  @Test
  void testCalculateRateCrossCourse() {
    Double sourceAmount = 3.0;
    Double resultAmount = sourceAmount * currencyRates.get(CurrencyValues.EUR) / currencyRates.get(CurrencyValues.KZT);
    CalculateResponse response = blockingStub.calculateRate(CalculateRequest.newBuilder()
                    .setSpendCurrency(CurrencyValues.EUR)
                    .setDesiredCurrency(CurrencyValues.KZT)
                    .setAmount(sourceAmount)
            .build());
    Double resultRate = response.getCalculatedAmount();
    Assertions.assertEquals(resultAmount, resultRate, 0.01);
  }

  @Test
  void testCalculateRateZeroValue() {
    Double sourceAmount = 0.0;
    CalculateResponse response = blockingStub.calculateRate(CalculateRequest.newBuilder()
                    .setSpendCurrency(CurrencyValues.EUR)
                    .setDesiredCurrency(CurrencyValues.KZT)
                    .setAmount(sourceAmount)
            .build());
    Double resultRate = response.getCalculatedAmount();

    Assertions.assertEquals(0.0, resultRate);
  }

  @Test
  void testCalculateRateUnspecifiedCurrency() {
    Assertions.assertThrows(StatusRuntimeException.class, () -> blockingStub.calculateRate(CalculateRequest.newBuilder()
            .setSpendCurrency(CurrencyValues.UNSPECIFIED)
            .setDesiredCurrency(CurrencyValues.KZT)
            .setAmount(3.0)
            .build()));
  }

  @Test
  void testCalculateRateUnspecifiedCurrencyDesired() {
    Assertions.assertThrows(StatusRuntimeException.class, () -> blockingStub.calculateRate(CalculateRequest.newBuilder()
            .setSpendCurrency(CurrencyValues.KZT)
            .setDesiredCurrency(CurrencyValues.UNSPECIFIED)
            .setAmount(3.0)
            .build()));
  }

  @Test
  void testCalculateMissingDesiredCurrency() {

    Assertions.assertThrows(StatusRuntimeException.class, () -> blockingStub.calculateRate(CalculateRequest.newBuilder()
            .setSpendCurrency(CurrencyValues.RUB)
            .setAmount(3.0)
            .build()));
  }

  @Test
  void testCalculateMissingSpendCurrency() {
    Assertions.assertThrows(StatusRuntimeException.class, () -> blockingStub.calculateRate(CalculateRequest.newBuilder()
            .setDesiredCurrency(CurrencyValues.RUB)
            .setAmount(3.0)
            .build()));
  }

  @Test
  void testCalculateRateMissingAmount() {
    Double sourceAmount = 0.0;
    CalculateResponse response = blockingStub.calculateRate(CalculateRequest.newBuilder()
            .setSpendCurrency(CurrencyValues.EUR)
            .setDesiredCurrency(CurrencyValues.KZT)
            .build());
    Double resultRate = response.getCalculatedAmount();

    Assertions.assertEquals(0.0, resultRate);
  }

}
