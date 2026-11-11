   package com.proyecto.reservalaboratorio.dto;

   /**
    * Indicadores de ocupación de un laboratorio en un rango de fechas.
    * Se calcula solo con reservas no canceladas.
    */
   public record IndicadorOcupacionResponse(
       Long laboratorioId,
       String laboratorio,
       Integer capacidad,
       long totalReservas,
       double horasReservadas,
       double horasDisponibles,
       double porcentajeOcupacion,
       double promedioAsistentes,
       double porcentajeUsoAforo
   ) {
   }