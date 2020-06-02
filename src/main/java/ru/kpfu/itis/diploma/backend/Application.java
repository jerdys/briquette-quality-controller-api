package ru.kpfu.itis.diploma.backend;

import ru.kpfu.itis.diploma.backend.model.User;
import ru.kpfu.itis.diploma.backend.repo.UserRepo;
import ru.kpfu.itis.diploma.backend.security.Role;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.bytedeco.javacpp.Loader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.PostConstruct;
import java.lang.ref.Cleaner;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@RequiredArgsConstructor
@SpringBootApplication
public class Application {
    public static final Cleaner CLEANER = Cleaner.create();

    static {
        long t0 = System.currentTimeMillis();
        Loader.load(org.bytedeco.opencv.opencv_java.class);
        log.info("OpenCV loaded in " + (System.currentTimeMillis() - t0) + "ms");
    }

    private final ExecutorService defaultExecutor = Executors.newCachedThreadPool(
            new ThreadFactory() {
                private final String namePrefix = "default-pool-";
                private final AtomicInteger threadNumber = new AtomicInteger(0);

                @Override
                public Thread newThread(@NonNull Runnable r) {
                    Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            }
    );

    @Bean
    public TaskScheduler taskScheduler() {
        final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("sched-pool-");
        scheduler.setDaemon(true);
        return scheduler;
    }

    @Bean
    public ExecutorService defaultExecutorService() {
        return defaultExecutor;
    }

    public static void main(String[] args) {
        System.setProperty("hibernate.types.print.banner", "false");
        SpringApplication.run(Application.class, args);
    }

    private final UserRepo userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    private void init() {
        Optional<User> userOptional = userRepository.findFirstByLoginAndDeletedIsFalse("admin");
        if (userOptional.isEmpty()) {
            userRepository.save(
                    User.builder()
                            .login("admin")
                            .name("admin")
                            .roles(Set.of(Role.values()))
                            .password(passwordEncoder.encode("admin"))
                            .deleted(false)
                            .build()
            );
        }
    }
}
