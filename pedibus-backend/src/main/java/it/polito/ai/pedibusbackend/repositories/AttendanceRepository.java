package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Attendance;
import it.polito.ai.pedibusbackend.entities.Pupil;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.sql.Date;
import java.util.Optional;

public interface AttendanceRepository extends CrudRepository<Attendance, Long> {
    @Query("SELECT a FROM Attendance " +
            "a JOIN a.ride r ON r.date = ?2 AND r.direction = ?3 " +
            "WHERE a.pupil = ?1")
    Optional<Attendance> findByPupilAndDateAndDirection(Pupil pupil, Date date, Character direction);
}
