package com.zongce.system.config;

import com.zongce.system.entity.AppUser;
import com.zongce.system.entity.enums.RoleType;
import com.zongce.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap.create-sample-users:false}")
    private boolean createSampleUsers;

    @Value("${app.bootstrap.admin-username:admin}")
    private String bootstrapAdminUsername;

    @Value("${app.bootstrap.admin-name:系统管理员}")
    private String bootstrapAdminName;

    @Value("${app.bootstrap.admin-department:信息化办公室}")
    private String bootstrapAdminDepartment;

    @Value("${app.user.initial-password:123456}")
    private String initialPassword;

    @Bean
    public CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            int migrated = migrateLegacyPlaintextPasswords(userRepository, passwordEncoder);
            if (migrated > 0) {
                log.warn("Migrated {} legacy plaintext password(s) to BCrypt.", migrated);
            }

            if (!bootstrapEnabled) {
                log.info("Bootstrap initialization is disabled.");
                return;
            }

            if (userRepository.count() > 0) {
                log.info("Existing users detected. Skip bootstrap initialization.");
                return;
            }

            createUserIfAbsent(
                    userRepository,
                    bootstrapAdminUsername,
                    bootstrapAdminName,
                    RoleType.ADMIN,
                    null,
                    bootstrapAdminDepartment,
                    passwordEncoder
            );

            if (createSampleUsers) {
                createUserIfAbsent(userRepository, "pe001", "体育教师", RoleType.TEACHER_PE, null, "体育教研室", passwordEncoder);
                createUserIfAbsent(userRepository, "study001", "智育教师", RoleType.TEACHER_STUDY, null, "教务办公室", passwordEncoder);
                createUserIfAbsent(userRepository, "counselor001", "辅导员", RoleType.COUNSELOR, "软件221", "学生工作办公室", passwordEncoder);
                createUserIfAbsent(userRepository, "stu001", "张三", RoleType.STUDENT, "软件221", null, passwordEncoder);
                createUserIfAbsent(userRepository, "stu002", "李四", RoleType.STUDENT, "软件221", null, passwordEncoder);
                log.warn("Bootstrap sample users were created because app.bootstrap.create-sample-users=true.");
            }

            log.warn("Bootstrap admin account created: username='{}'. Please change the initial password after first login.", bootstrapAdminUsername);
        };
    }

    private int migrateLegacyPlaintextPasswords(UserRepository userRepository,
                                                PasswordEncoder passwordEncoder) {
        List<AppUser> users = userRepository.findAll();
        int migrated = 0;
        for (AppUser user : users) {
            String stored = user.getPassword();
            if (!StringUtils.hasText(stored)) {
                continue;
            }
            if (isBcryptHash(stored)) {
                continue;
            }
            user.setPassword(passwordEncoder.encode(stored.trim()));
            userRepository.save(user);
            migrated++;
        }
        return migrated;
    }

    private boolean isBcryptHash(String value) {
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }

    private void createUserIfAbsent(UserRepository userRepository,
                                    String username,
                                    String name,
                                    RoleType role,
                                    String className,
                                    String department,
                                    PasswordEncoder passwordEncoder) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setName(name);
        user.setRole(role);
        user.setClassName(className);
        user.setDepartment(department);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(initialPassword));
        userRepository.save(user);
    }
}
