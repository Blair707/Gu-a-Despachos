package com.transporte.guias.repository;

import com.transporte.guias.model.GuiaDespacho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {

    Optional<GuiaDespacho> findByNumeroGuia(String numeroGuia);

    List<GuiaDespacho> findByTransportista(String transportista);

    List<GuiaDespacho> findByFechaDespacho(LocalDate fechaDespacho);

    List<GuiaDespacho> findByTransportistaAndFechaDespacho(String transportista, LocalDate fechaDespacho);

    List<GuiaDespacho> findByTransportistaAndFechaDespachoGreaterThanEqual(String transportista, LocalDate desde);

    boolean existsByNumeroGuia(String numeroGuia);
}
