package com.vitsync.iam.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * El cliente de HaveIBeenPwned contra un servidor simulado: sin red, en milisegundos.
 *
 * <p>SHA-1 de "password" = 5BAA6 1E4C9B93F3F0682250B6CF8331B7EE68FD8: solo viaja el prefijo.
 */
class PwnedPasswordsClientTest {

    private static final String RANGE_URL = "https://hibp.test/range/5BAA6";
    private static final String SUFFIX = "1E4C9B93F3F0682250B6CF8331B7EE68FD8";

    private MockRestServiceServer server;
    private PwnedPasswordsClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://hibp.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new PwnedPasswordsClient(builder.build());
    }

    @Test
    void only_the_5_char_prefix_is_sent_and_a_matching_suffix_means_breached() {
        server.expect(requestTo(RANGE_URL))
                .andExpect(header("Add-Padding", "true"))
                .andRespond(withSuccess("0018A45C4D1DEF81644B54AB7F969B88D65:1\r\n" + SUFFIX + ":9545824\r\n",
                        MediaType.TEXT_PLAIN));

        assertThat(client.isBreached("password")).isTrue();
        server.verify();
    }

    @Test
    void padding_entries_with_count_zero_do_not_count() {
        server.expect(requestTo(RANGE_URL))
                .andRespond(withSuccess(SUFFIX + ":0\r\n", MediaType.TEXT_PLAIN));

        assertThat(client.isBreached("password")).isFalse();
    }

    @Test
    void suffix_not_in_the_range_means_not_breached() {
        server.expect(requestTo(RANGE_URL))
                .andRespond(withSuccess("0018A45C4D1DEF81644B54AB7F969B88D65:1\r\n", MediaType.TEXT_PLAIN));

        assertThat(client.isBreached("password")).isFalse();
    }

    @Test
    void service_error_fails_open() {
        server.expect(requestTo(RANGE_URL)).andRespond(withServerError());

        assertThat(client.isBreached("password")).isFalse();
    }

    @Test
    void network_failure_fails_open() {
        server.expect(requestTo(RANGE_URL)).andRespond(request -> {
            throw new ResourceAccessException("timeout");
        });

        assertThat(client.isBreached("password")).isFalse();
    }
}
