import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.response.Response;
import org.apache.commons.validator.routines.UrlValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckCatContract {
    WireMockServer wireMockServer;
    private static final String REAL_CAT_API = "https://api.thecatapi.com";
    private static final String MOCKED_CAT_API = "http://localhost:8090";
    private static final String BRITISH_LONG_HAIR = "/v1/images/search?breed_ids=bslo";
    private static final String realFullyQualifiedUrl = REAL_CAT_API + BRITISH_LONG_HAIR;
    private static final String mockedFullyQualifiedUrl = MOCKED_CAT_API + BRITISH_LONG_HAIR;

    UrlValidator urlValidator = new UrlValidator();

    @BeforeEach
    public void setup() {
        /**
         * The stubs doesnt really need to be embedded. Im doing it in here to be easier to understand.
         * You can have a standAlone mocks server running in a different process/service/places if you like.
         * Wiremock, mountebank, postman or any other mocking tooling you like.
         */
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        setupStubs();
    }

    @AfterEach
    private void teardown() {
        wireMockServer.stop();
    }

    // Adding mock for the search by breed endpoint and filtering by bslo - british long hair
    private void setupStubs() {
        wireMockServer.stubFor(get(urlEqualTo(BRITISH_LONG_HAIR))
                .willReturn(aResponse().withHeader("Content-Type", "application-json")
                        .withStatus(200)
                        .withBodyFile("json/british-long-hair.json")));
    }

    @Test
    @DisplayName("Check if contract is kept for the affection level and image url comparing both results")
    public void checkBreedContractComparingResponses() {
        Response realResponse, mockedResponse;
        int realAffectionLevel, mockedAffectionLevel;
        String realCatUrl, mockedCatUrl;
        String affectionLevelJsonPath = "breeds.affection_level[0][0]";
        String catJsonPath = "url[0]";

        realResponse = getCat(realFullyQualifiedUrl);
        mockedResponse = getCat(mockedFullyQualifiedUrl);

        realAffectionLevel = realResponse.jsonPath().get(affectionLevelJsonPath);
        mockedAffectionLevel = mockedResponse.jsonPath().get(affectionLevelJsonPath);

        realCatUrl = realResponse.jsonPath().get(catJsonPath);
        mockedCatUrl = mockedResponse.jsonPath().get(catJsonPath);

        assertThat(realAffectionLevel, equalTo(mockedAffectionLevel));
        assertTrue(urlValidator.isValid(realCatUrl), "Url is valid");
        assertTrue(urlValidator.isValid(mockedCatUrl), "Url is valid");
    }


    public Response getCat(String fullyQualifiedUrl) {
        return when()
                .get(fullyQualifiedUrl)
               .then()
                .extract().response();
    }

}
