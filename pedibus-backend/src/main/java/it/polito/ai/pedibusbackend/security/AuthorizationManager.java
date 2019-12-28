package it.polito.ai.pedibusbackend.security;

import it.polito.ai.pedibusbackend.entities.*;
import it.polito.ai.pedibusbackend.exceptions.ForbiddenException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AuthorizationManager {
    public static void authorizeReservationAccess(User user, Reservation reservation) throws ForbiddenException {
        if(!user.getEmail().equals(reservation.getPupil().getUser().getEmail())){
            authorizeLineAccess(user, reservation.getStop().getLine());
        }
    }

    public static void authorizeRideConductor(User user, Ride ride, Availability availability) throws ForbiddenException{
        if(!user.getRoles().contains("ROLE_SYSTEM-ADMIN")){
            if(!(user.getLines().stream()
                    .map((l)-> l.getId())
                    .collect(Collectors.toList())
                    .contains(ride.getLine().getId()))){
                if(!availability.getStatus().equals("CONSOLIDATED")){
                    throw new ForbiddenException("The user is not allowed to do this action");
                }
            }
        }
    }

    public static void authorizeLineAccess(User user, Line line) throws ForbiddenException {
        authorizeLineAccess(user, line.getId());
    }

    public static void authorizeLineAccess(User user, Long lineId) throws ForbiddenException {
        if(!user.getRoles().contains("ROLE_SYSTEM-ADMIN")) {
            if(!(user.getLines().stream()
                    .map((l)-> l.getId())
                    .collect(Collectors.toList())
                    .contains(lineId))){
                throw new ForbiddenException("The user is not allowed to do this action");
            }
        }
    }

    public static void authorizeLinesAccess(User user, Set<Long> lineIds) throws ForbiddenException {
        if(!user.getRoles().contains("ROLE_SYSTEM-ADMIN")) {
            if(!(user.getLines().stream()
                    .map((l)-> l.getId())
                    .collect(Collectors.toList())
                    .containsAll(lineIds))) {
                throw new ForbiddenException("The user is not allowed to do this action");
            }
        }
    }
}
