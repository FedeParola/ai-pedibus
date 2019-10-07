package it.polito.ai.pedibusbackend.repositories;

import it.polito.ai.pedibusbackend.entities.Attendance;
import org.springframework.data.repository.CrudRepository;

public interface AttendanceRepository extends CrudRepository<Attendance, Long> {
}
