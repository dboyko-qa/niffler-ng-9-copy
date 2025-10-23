package guru.qa.niffler.test.gql;

import com.apollographql.apollo.api.ApolloResponse;
import com.apollographql.apollo.api.Error;
import com.apollographql.java.client.ApolloCall;
import com.apollographql.java.rx2.Rx2Apollo;
import guru.qa.*;
import guru.qa.niffler.jupiter.annotation.ApiLogin;
import guru.qa.niffler.jupiter.annotation.Token;
import guru.qa.niffler.jupiter.annotation.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class UsersGraphqlTest extends  BaseGraphQlTest {

    @User(
        friends = 1
    )
    @Test
    @ApiLogin
    void cannotQueryCategoriesForAnotherUserTest(@Token String bearerToken) {
        final ApolloCall<FriendsWithCategoriesQuery.Data> friendsCall = apolloClient.query(FriendsWithCategoriesQuery.builder()
                        .build())
                .addHttpHeader("authorization", bearerToken);

        final ApolloResponse<FriendsWithCategoriesQuery.Data> response = Rx2Apollo.single(friendsCall).blockingGet();
        List<Error> errors = response.errors;
        Assertions.assertNotNull(errors);
        Assertions.assertFalse(response.errors.isEmpty());
        Assertions.assertTrue(response.errors.stream().anyMatch(er -> er.getMessage().equals("Can`t query categories for another user")));
    }

    @User(
        friends = 1
    )
    @Test
    @ApiLogin
    void verifyInternalRequestDepth(@Token String bearerToken) {
        final ApolloCall<Friends2SubQueriesQuery.Data> friendsCall = apolloClient.query(Friends2SubQueriesQuery.builder()
                        .build())
                .addHttpHeader("authorization", bearerToken);

        final ApolloResponse<Friends2SubQueriesQuery.Data> response = Rx2Apollo.single(friendsCall).blockingGet();
        List<Error> errors = response.errors;
        Assertions.assertNotNull(errors);
        Assertions.assertFalse(response.errors.isEmpty());
        Assertions.assertTrue(response.errors.stream().anyMatch(er -> er.getMessage().equals("Can`t fetch over 2 friends sub-queries")));
    }
}
