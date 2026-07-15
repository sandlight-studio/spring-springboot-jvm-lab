package studio.sandlight.app.domain

import jakarta.persistence.*
import java.time.Instant

/**
 * A regular class (not a data class): JPA entities have identity beyond their
 * fields, and data-class equals/hashCode/copy interact badly with proxies and
 * generated ids. Input validation lives on the web DTO (CreateUserRequest),
 * not here — annotations on the entity would never be triggered by save().
 */
@Entity
@Table(name = "users")
class User(
    var name: String,
    @Column(unique = true)
    var email: String,
) {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    var createdAt: Instant = Instant.now()
}
