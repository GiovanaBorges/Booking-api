package com.booking.booking.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;

import com.booking.booking.ENUMS.RolesENUM;
import com.booking.booking.ENUMS.StatusENUM;
import com.booking.booking.ENUMS.TechSkillsENUM;
import com.booking.booking.models.Bookings;
import com.booking.booking.models.ProviderAvailability;
import com.booking.booking.models.Users;
import com.booking.booking.repositories.BookingsRepository;
import com.booking.booking.repositories.ProviderAvailabilityRepository;
import com.booking.booking.repositories.UsersRepository;

import org.springframework.boot.CommandLineRunner;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class DataSeeder {

    private final UsersRepository usersRepo;
    private final BookingsRepository bookingRepo;
    private final ProviderAvailabilityRepository availabilityRepo;

    @Bean
    CommandLineRunner seedDatabase() {
        Random random = new Random();
        return args -> {

            if (usersRepo.count() > 0)
                return;

            List<Map<String, String>> keycloakUsers = List.of(
                    Map.of("username", "ana.silva@test.com", "role", "CLIENT"),
                    Map.of("username", "bruno.costa@test.com", "role", "PROVIDER"),
                    Map.of("username", "carla.rocha@test.com", "role", "CLIENT"),
                    Map.of("username", "daniel.alves@test.com", "role", "PROVIDER"),
                    Map.of("username", "eduarda.melo@test.com", "role", "CLIENT"),
                    Map.of("username", "felipe.ribeiro@test.com", "role", "PROVIDER"),
                    Map.of("username", "gabriela.santos@test.com", "role", "CLIENT"),
                    Map.of("username", "henrique.lima@test.com", "role", "PROVIDER"),
                    Map.of("username", "isabela.teixeira@test.com", "role", "CLIENT"),
                    Map.of("username", "joao.pereira@test.com", "role", "PROVIDER"),
                    Map.of("username", "karina.gomes@test.com", "role", "CLIENT"),
                    Map.of("username", "lucas.martins@test.com", "role", "PROVIDER"),
                    Map.of("username", "mariana.fernandes@test.com", "role", "CLIENT"),
                    Map.of("username", "nicolas.barros@test.com", "role", "PROVIDER"),
                    Map.of("username", "olivia.cardoso@test.com", "role", "CLIENT"),
                    Map.of("username", "paulo.moreira@test.com", "role", "PROVIDER"),
                    Map.of("username", "rafaela.duarte@test.com", "role", "CLIENT"),
                    Map.of("username", "samuel.freitas@test.com", "role", "PROVIDER"),
                    Map.of("username", "tatiane.araujo@test.com", "role", "CLIENT"),
                    Map.of("username", "vinicius.batista@test.com", "role", "PROVIDER"));

            List<Users> clients = new ArrayList<>();
            List<Users> providers = new ArrayList<>();

            List<TechSkillsENUM> allSkills = Arrays.asList(TechSkillsENUM.values());

            Collections.shuffle(allSkills);

            int quantidadeSkills = 3 + random.nextInt(3); // 3 a 5

            Set<TechSkillsENUM> skillsSorteadas = new HashSet<>(
                allSkills.subList(0, quantidadeSkills)
            );

            for (Map<String, String> u : keycloakUsers) {

                RolesENUM role = RolesENUM.valueOf(u.get("role"));

                Users user = Users.builder()
                        .keycloakId(u.get("username"))
                        .email(u.get("username"))
                        .name(u.get("username").split("@")[0])
                        .roles(role)
                        .experienceYears(2 + random.nextInt(11)) 
                        .skills(skillsSorteadas)
                        .createdAt(LocalDateTime.now())
                        .build();

                usersRepo.save(user);

                if (role == RolesENUM.PROVIDER)
                    providers.add(user);
                else
                    clients.add(user);
            }

            // availability
            for (Users provider : providers) {

                for (int i = 0; i < 5; i++) {

                    int dayOfWeek = random.nextInt(7) + 1; // 1 a 7

                    int startHour = 8 + random.nextInt(8); // entre 8h e 15h
                    int duration = 1 + random.nextInt(3); // duração de 1 a 3 horas

                    LocalTime start = LocalTime.of(startHour, 0);
                    LocalTime end = start.plusHours(duration);

                    ProviderAvailability a = ProviderAvailability.builder()
                            .provider(provider)
                            .dayOfWeek(dayOfWeek)
                            .startTime(start)
                            .endTime(end)
                            .build();

                    availabilityRepo.save(a);
                }
            }

            // bookings

            for (Users client : clients) {

                Users provider = providers.get(random.nextInt(providers.size()));

                LocalDateTime start = LocalDateTime.now().plusDays(random.nextInt(5) + 1);
                LocalDateTime end = start.plusHours(1);

                Bookings booking = Bookings.builder()
                        .customer(client)
                        .provider(provider)
                        .startTs(start)
                        .endTs(end)
                        .status(StatusENUM.CONFIRMED)
                        .title("Consulta")
                        .description("Agendamento automático")
                        .build();

                bookingRepo.save(booking);
            }

            System.out.println("🌱 Banco populado com sucesso!");
        };
    }
}