package com.example.sis1.persistence.repository;

import com.example.sis1.domain.model.TaskPriority;
import com.example.sis1.domain.model.TaskStatus;
import com.example.sis1.persistence.entity.TaskEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TaskRepositoryIntegrationTest {

    private static final String HOST = env("TEST_DB_HOST", "localhost");
    private static final String PORT = env("TEST_DB_PORT", "5432");
    private static final String USER = env("TEST_DB_USERNAME", "postgres");
    private static final String PASSWORD = env("TEST_DB_PASSWORD", "postgres");
    private static final String DB_NAME = "sis1_test";

    private static final String ADMIN_URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/postgres";
    private static final String TEST_DB_URL = "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB_NAME;

    @BeforeAll
    static void requireLocalPostgres() {
        try {
            createTestDatabaseIfMissing();
        } catch (SQLException e) {
            assumeTrue(false, "Local PostgreSQL is not reachable at " + HOST + ":" + PORT
                    + " (user '" + USER + "') - skipping the repository integration test: " + e.getMessage());
        }
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> TEST_DB_URL);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private TaskRepository taskRepository;

    @Test
    void save_generatesIdAndFillsAuditFieldsAndDefaults() {
        TaskEntity saved = taskRepository.saveAndFlush(task("Write report"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(saved.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    }

    @Test
    void findById_returnsWhatWasStored() {
        TaskEntity saved = taskRepository.saveAndFlush(task("Write report"));

        TaskEntity found = taskRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("Write report");
    }

    @Test
    void findByStatus_returnsOnlyMatchingTasksAndRespectsPaging() {
        taskRepository.save(task("New one", TaskStatus.NEW));
        taskRepository.save(task("Done one", TaskStatus.DONE));
        taskRepository.save(task("Done two", TaskStatus.DONE));
        taskRepository.save(task("Done three", TaskStatus.DONE));
        taskRepository.flush();

        Page<TaskEntity> firstPage = taskRepository.findByStatus(
                TaskStatus.DONE, PageRequest.of(0, 2, Sort.by("id")));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getContent()).allMatch(t -> t.getStatus() == TaskStatus.DONE);
    }

    @Test
    void existsByTitleIgnoreCase_ignoresCase() {
        taskRepository.saveAndFlush(task("Prepare Report"));

        assertThat(taskRepository.existsByTitleIgnoreCase("prepare report")).isTrue();
        assertThat(taskRepository.existsByTitleIgnoreCase("PREPARE REPORT")).isTrue();
        assertThat(taskRepository.existsByTitleIgnoreCase("something else")).isFalse();
    }

    @Test
    void existsByTitleIgnoreCaseAndIdNot_excludesTheTasksOwnRow() {
        TaskEntity first = taskRepository.saveAndFlush(task("First"));
        TaskEntity second = taskRepository.saveAndFlush(task("Second"));

        assertThat(taskRepository.existsByTitleIgnoreCaseAndIdNot("First", first.getId())).isFalse();
        assertThat(taskRepository.existsByTitleIgnoreCaseAndIdNot("First", second.getId())).isTrue();
    }

    @Test
    void uniqueTitleConstraint_isEnforcedByTheDatabase() {
        taskRepository.saveAndFlush(task("Unique title"));

        assertThatThrownBy(() -> taskRepository.saveAndFlush(task("Unique title")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void delete_removesTheRow() {
        TaskEntity saved = taskRepository.saveAndFlush(task("To delete"));

        taskRepository.delete(saved);
        taskRepository.flush();

        assertThat(taskRepository.findById(saved.getId())).isEmpty();
    }


    private static void createTestDatabaseIfMissing() throws SQLException {
        try (Connection connection = DriverManager.getConnection(ADMIN_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            boolean exists;
            try (ResultSet rs = statement.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + DB_NAME + "'")) {
                exists = rs.next();
            }
            if (!exists) {
                statement.execute("CREATE DATABASE " + DB_NAME);
            }
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private TaskEntity task(String title) {
        return TaskEntity.builder().title(title).build();
    }

    private TaskEntity task(String title, TaskStatus status) {
        return TaskEntity.builder().title(title).status(status).build();
    }
}
