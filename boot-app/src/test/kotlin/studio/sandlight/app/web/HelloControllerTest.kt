package studio.sandlight.app.web

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
@TestPropertySource(
    properties = [
        "app.greeting=Hi",
        "app.feature-flag=true",
        "spring.application.name=test-app",
    ],
)
class HelloControllerTest(
    @Autowired private val wac: WebApplicationContext,
) {

    @Test
    fun `GET api hello returns expected shape`() {
        val mvc = MockMvcBuilders.webAppContextSetup(wac).build()
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
