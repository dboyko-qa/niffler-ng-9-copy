package guru.qa.niffler.test.gql;

import com.apollographql.apollo.api.ApolloResponse;
import com.apollographql.java.client.ApolloCall;
import com.apollographql.java.rx2.Rx2Apollo;
import guru.qa.StatQuery;
import guru.qa.niffler.jupiter.annotation.*;
import guru.qa.type.CurrencyValues;
import guru.qa.type.FilterPeriod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class StatGraphQlTest extends BaseGraphQlTest {

  @User
  @Test
  @ApiLogin
  void statTest(@Token String bearerToken) {
    final ApolloCall<StatQuery.Data> currenciesCall = apolloClient.query(StatQuery.builder()
            .filterCurrency(null)
            .statCurrency(null)
            .filterPeriod(null)
            .build())
        .addHttpHeader("authorization", bearerToken);

    final ApolloResponse<StatQuery.Data> response = Rx2Apollo.single(currenciesCall).blockingGet();
    final StatQuery.Data data = response.dataOrThrow();
    StatQuery.Stat result = data.stat;
    Assertions.assertEquals(
        0.0,
        result.total
    );
  }

  @User(
          categories = {
                  @Category(name = "Category1"),
                  @Category(name = "Category2")
          },
          spendings = {
                  @Spending(category = "Category1", description = "расход1", amount = 1001, daysMinus = 2),
                  @Spending(category = "Category2", description = "расход2", amount = 1002), //filtered out
                  @Spending(category = "Category1", description = "расход1", amount = 2003, currency = guru.qa.niffler.model.CurrencyValues.EUR)
          }
  )
  @Test
  @ApiLogin
  void testFilterTodayAndCurrency(@Token String bearerToken) {
    final ApolloCall<StatQuery.Data> currenciesCall = apolloClient.query(StatQuery.builder()
            .filterCurrency(CurrencyValues.RUB)
            .statCurrency(null)
            .filterPeriod(FilterPeriod.TODAY)
            .build())
        .addHttpHeader("authorization", bearerToken);

    final ApolloResponse<StatQuery.Data> response = Rx2Apollo.single(currenciesCall).blockingGet();
    final StatQuery.Data data = response.dataOrThrow();
    StatQuery.Stat result = data.stat;
    Assertions.assertEquals(1002.0, result.total);
    Assertions.assertEquals("Category2", result.statByCategories.get(0).categoryName);
  }

  @User(
          categories = {
                  @Category(name = "Category1"),
                  @Category(name = "Category2"),
                  @Category(name = "Category3")
          },
          spendings = {
                  @Spending(category = "Category1", description = "расход1", amount = 1001, daysMinus = 6),
                  @Spending(category = "Category2", description = "расход2", amount = 2002, daysMinus = 2, currency = guru.qa.niffler.model.CurrencyValues.EUR),//filtered out
                  @Spending(category = "Category3", description = "расход3", amount = 1002, daysMinus = 7),
                  @Spending(category = "Category3", description = "расход3", amount = 10005, daysMinus = 8)//filtered out
          }
  )
  @Test
  @ApiLogin
  void testFilterWeekAndConversion(@Token String bearerToken) {
    final ApolloCall<StatQuery.Data> currenciesCall = apolloClient.query(StatQuery.builder()
            .filterCurrency(CurrencyValues.RUB)
            .statCurrency(CurrencyValues.EUR)
            .filterPeriod(FilterPeriod.WEEK)
            .build())
        .addHttpHeader("authorization", bearerToken);

    final ApolloResponse<StatQuery.Data> response = Rx2Apollo.single(currenciesCall).blockingGet();
    final StatQuery.Data data = response.dataOrThrow();
    StatQuery.Stat result = data.stat;
    Map<String, Double> expectedCategories = Map.of("Category1", 13.9, "Category3", 13.92);
    Assertions.assertEquals(27.82, result.total);
    Assertions.assertEquals(expectedCategories.size(), result.statByCategories.size());
    Assertions.assertTrue(result.statByCategories.stream().allMatch(cat ->
            expectedCategories.containsKey(cat.categoryName) &&
                    expectedCategories.get(cat.categoryName).equals(cat.sum)
    ));
  }

  @User(
          categories = {
                  @Category(name = "Category1"),
                  @Category(name = "Category2"),
                  @Category(name = "Category3", archived = true),
                  @Category(name = "Category4", archived = true),
                  @Category(name = "Category5")
          },
          spendings = {
                  @Spending(category = "Category1", description = "расход1", amount = 1001),
                  @Spending(category = "Category2", description = "расход2", amount = 2002, daysMinus = 2, currency = guru.qa.niffler.model.CurrencyValues.EUR),
                  @Spending(category = "Category3", description = "расход3", amount = 1002, daysMinus = 7, currency = guru.qa.niffler.model.CurrencyValues.KZT),
                  @Spending(category = "Category3", description = "расход4", amount = 10005, daysMinus = 8),
                  @Spending(category = "Category4", description = "расход5", amount = 3003, daysMinus = 30),
                  @Spending(category = "Category5", description = "расход6", amount = 4004, daysMinus = 31),//filtered out
                  @Spending(category = "Category4", description = "расход7", amount = 5005, daysMinus = 45),//filtered out
          }
  )
  @Test
  @ApiLogin
  void testFilterMonthAndArchivedCategoryAndMultiConversion(@Token String bearerToken) {
    final ApolloCall<StatQuery.Data> currenciesCall = apolloClient.query(StatQuery.builder()
            .filterCurrency(null)
            .statCurrency(CurrencyValues.USD)
            .filterPeriod(FilterPeriod.MONTH)
            .build())
        .addHttpHeader("authorization", bearerToken);

    final ApolloResponse<StatQuery.Data> response = Rx2Apollo.single(currenciesCall).blockingGet();
    final StatQuery.Data data = response.dataOrThrow();
    StatQuery.Stat result = data.stat;
    Map<String, Double> expectedCategories = Map.of("Category1", 15.02,
            "Category2", 2162.16,
            "Archived", 197.22);
    Assertions.assertEquals(2374.4, result.total);
    Assertions.assertEquals(expectedCategories.size(), result.statByCategories.size());
    Assertions.assertTrue(result.statByCategories.stream().allMatch(cat ->
            expectedCategories.containsKey(cat.categoryName) &&
                    expectedCategories.get(cat.categoryName).equals(cat.sum)
    ));
  }

  @User(
          categories = {
                  @Category(name = "Category1"),
                  @Category(name = "Category2"),
                  @Category(name = "Category3", archived = true),
                  @Category(name = "Category4", archived = true),
                  @Category(name = "Category5")
          },
          spendings = {
                  @Spending(category = "Category1", description = "расход1", amount = 1001),
                  @Spending(category = "Category2", description = "расход2", amount = 2002, daysMinus = 50, currency = guru.qa.niffler.model.CurrencyValues.EUR),
                  @Spending(category = "Category3", description = "расход3", amount = 1002, daysMinus = 7, currency = guru.qa.niffler.model.CurrencyValues.KZT),
                  @Spending(category = "Category3", description = "расход4", amount = 10005, daysMinus = 8),
                  @Spending(category = "Category4", description = "расход5", amount = 3003, daysMinus = 30),
                  @Spending(category = "Category5", description = "расход6", amount = 4004, daysMinus = 31),
                  @Spending(category = "Category4", description = "расход7", amount = 5005, daysMinus = 45),
          }
  )
  @Test
  @ApiLogin
  void testNoTimeFilterAndArchivedCategoryAndMultiConversion(@Token String bearerToken) {
    final ApolloCall<StatQuery.Data> currenciesCall = apolloClient.query(StatQuery.builder()
            .filterCurrency(null)
            .statCurrency(null)
            .filterPeriod(null)
            .build())
        .addHttpHeader("authorization", bearerToken);

    final ApolloResponse<StatQuery.Data> response = Rx2Apollo.single(currenciesCall).blockingGet();
    final StatQuery.Data data = response.dataOrThrow();
    StatQuery.Stat result = data.stat;
    System.out.println(result);
    Map<String, Double> expectedCategories = Map.of(
            "Category1", 1001.0,
            "Category2", 144144.0,
            "Archived", 18153.28,
            "Category5", 4004.0);
    Assertions.assertEquals(167302.28, result.total);
    Assertions.assertEquals(expectedCategories.size(), result.statByCategories.size());
    Assertions.assertTrue(result.statByCategories.stream().allMatch(cat ->
      expectedCategories.containsKey(cat.categoryName) &&
      expectedCategories.get(cat.categoryName).equals(cat.sum)
    ));
  }

}
