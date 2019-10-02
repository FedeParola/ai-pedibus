package it.polito.ai.pedibusbackend.viewmodels;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReservationsDTO {
    private final List<StopReservations> outwardReservations = new ArrayList<>();
    private final List<Pupil> outwardNoRes = new ArrayList<>();
    private final List<StopReservations> returnReservations = new ArrayList<>();
    private final List<Pupil> returnNoRes = new ArrayList<>();

    @Data
    public static class StopReservations {
        private String stopName;
        private String stopTime;
        private final List<Pupil> pupils = new ArrayList<>();

        public void addPupil(Long id, String name, Long attendanceId) {
            pupils.add(new Pupil(id, name, attendanceId));
        }
    }

    @Data
    public static class Pupil {
        private final Long id;
        private final String name;
        private final Long attendanceId;
    }
}
