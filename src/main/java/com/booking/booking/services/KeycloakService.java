package com.booking.booking.services;

import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;

@Service
public class KeycloakService {

    private final Keycloak keycloak;

    public KeycloakService() {
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080")
                .realm("master")
                .clientId("admin-cli")
                .username("admin")
                .password("admin123")
                .build();
    }

    public void assignGroup(String userId, String groupName) {

        RealmResource realm = keycloak.realm("Bookings");

        List<GroupRepresentation> groups = realm.groups().groups();

        GroupRepresentation group = groups.stream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado: " + groupName));

        var userResource = realm.users().get(userId);

        List<GroupRepresentation> userGroups = userResource.groups();

        boolean alreadyInGroup = userGroups.stream()
                .anyMatch(g -> g.getId().equals(group.getId()));

        if (!alreadyInGroup) {
            userResource.joinGroup(group.getId());
        }
    }

    public void assignRoleToUser(String userId, String roleName) {

        RealmResource realm = keycloak.realm("Bookings");

        RoleRepresentation role = realm.roles()
                .get(roleName)
                .toRepresentation();

        realm.users()
                .get(userId)
                .roles()
                .realmLevel()
                .add(List.of(role));
    }
}