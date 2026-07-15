package studio.sandlight.app.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

/**
 * @AutoConfigureMockMvc lets Boot build and inject the MockMvc instance;
 * no manual MockMvcBuilders wiring per test class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "app.greeting=Hi",
        "app.feature-flag=true",
        "spring.application.name=test-app",
    ],
)
class HelloControllerTest(
    @param:Autowired private val mvc: MockMvc,
) {

    @Test
    fun `GET api hello returns expected shape`() {
        mvc.get("/api/hello") {
            accept = MediaType.APPLICATION_JSON
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.message") { value("Hi from Spring Boot") }
                jsonPath("$.featureFlag") { value(true) }
                jsonPath("$.profiles") { isArray() }
                jsonPath("$.app") { value("test-app") }
            }
    }
}
