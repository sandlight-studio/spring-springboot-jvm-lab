package studio.sandlight.app.web

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@SpringBootTest
class UserControllerIT(
    @Autowired private val wac: WebApplicationContext,
) {

    @Test
    fun `POST users creates user and duplicate email returns 400`() {
        val mvc = MockMvcBuilders.webAppContextSetup(wac).build()
        val email = "it-${System.nanoTime()}@example.com"
        val body = """{"name":"Jane","email":"$email"}"""

        val created = mvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isCreated() }
        }.andReturn()

        assertTrue(created.response.contentAsString.contains(email))

        val duplicate = mvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            accept = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isBadRequest() }
        }.andReturn()

        assertTrue(duplicate.response.contentAsString.contains("email already exists"))
    }
}

