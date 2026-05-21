package id.ac.ui.cs.advprog.mysawit.auth.config;

import id.ac.ui.cs.advprog.mysawit.auth.entity.Assignment;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthProvider;
import id.ac.ui.cs.advprog.mysawit.auth.entity.AuthUser;
import id.ac.ui.cs.advprog.mysawit.auth.entity.Role;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AssignmentRepository;
import id.ac.ui.cs.advprog.mysawit.auth.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsvLocalSeedRunner implements CommandLineRunner {

    private final AuthUserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${seed.local:false}")
    private boolean seedLocal;

    @Value("${seed.users-file:seed/users.csv}")
    private String usersFile;

    @Value("${seed.assignments-file:seed/assignments.csv}")
    private String assignmentsFile;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!seedLocal) {
            return;
        }

        seedUsers(Path.of(usersFile));
        seedAssignments(Path.of(assignmentsFile));
    }

    private void seedUsers(Path path) throws IOException {
        if (!Files.exists(path)) {
            log.info("User seed file not found: {}", path);
            return;
        }

        int count = 0;
        for (List<String> row : readCsv(path)) {
            if (row.size() < 8) {
                throw new IllegalArgumentException(
                        "Invalid users seed row. Expected 8 columns: " + row);
            }

            String email = required(row.get(0), "email");
            String username = required(row.get(1), "username");
            String nama = required(row.get(2), "nama");
            Role role = Role.valueOf(required(row.get(3), "role"));
            String rawPassword = row.get(4).trim();
            String cert = blankToNull(row.get(5));
            BigDecimal walletBalance = parseWalletBalance(row.get(6));
            AuthProvider authProvider = parseAuthProvider(row.get(7));

            AuthUser user = userRepository.findByEmail(email).orElseGet(AuthUser::new);
            user.setEmail(email);
            user.setUsername(username);
            user.setNama(nama);
            user.setRole(role);
            user.setMandorCertificationNumber(role == Role.MANDOR ? cert : null);
            user.setWalletBalance(walletBalance);
            user.setAuthProvider(authProvider);

            if (authProvider == AuthProvider.LOCAL) {
                user.setPassword(passwordEncoder.encode(
                        required(rawPassword, "password")));
            } else if (!rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            } else {
                user.setPassword(null);
            }

            userRepository.save(user);
            count++;
        }

        log.info("Seeded {} users from {}", count, path);
    }

    private void seedAssignments(Path path) throws IOException {
        if (!Files.exists(path)) {
            log.info("Assignment seed file not found: {}", path);
            return;
        }

        int count = 0;
        for (List<String> row : readCsv(path)) {
            if (row.size() < 3) {
                throw new IllegalArgumentException(
                        "Invalid assignments seed row. Expected 3 columns: " + row);
            }

            AuthUser buruh = findUser(required(row.get(0), "buruhEmail"));
            AuthUser mandor = findUser(required(row.get(1), "mandorEmail"));
            String plantationId = blankToNull(row.get(2));

            if (buruh.getRole() != Role.BURUH) {
                throw new IllegalArgumentException(
                        "Assignment buruhEmail must point to a BURUH user: "
                                + buruh.getEmail());
            }
            if (mandor.getRole() != Role.MANDOR) {
                throw new IllegalArgumentException(
                        "Assignment mandorEmail must point to a MANDOR user: "
                                + mandor.getEmail());
            }
            if (assignmentRepository.existsByBuruh(buruh)) {
                continue;
            }

            assignmentRepository.save(Assignment.builder()
                    .buruh(buruh)
                    .mandor(mandor)
                    .plantationId(plantationId)
                    .build());
            count++;
        }

        log.info("Seeded {} assignments from {}", count, path);
    }

    private AuthUser findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Seed user not found: " + email));
    }

    private List<List<String>> readCsv(Path path) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                rows.add(parseCsvLine(line));
            }
        }
        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append(character);
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        values.add(current.toString().trim());
        return values;
    }

    private String required(String value, String column) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required seed column: " + column);
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal parseWalletBalance(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? BigDecimal.ZERO : new BigDecimal(normalized);
    }

    private AuthProvider parseAuthProvider(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? AuthProvider.LOCAL : AuthProvider.valueOf(normalized);
    }
}
