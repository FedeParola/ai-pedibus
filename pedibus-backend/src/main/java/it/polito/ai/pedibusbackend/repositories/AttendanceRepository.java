package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Attendance;
import it.polito.ai.pedibusbackend.entities.Pupil;
import org.springframework.data.repository.CrudRepository;

import java.sql.Date;
import java.util.Optional;

public interface AttendanceRepository extends CrudRepository<Attendance, Long> {
    Optional<Attendance> getByPupilAndDateAndDirection(Pupil pupil, Date date, Character direction);
}
