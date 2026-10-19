package ar.edu.iua.iw3.model;

/**
 * Maquina de estados de la orden.
 * Las transiciones son secuenciales y unidireccionales: solo se puede pasar al estado siguiente.
 */
public enum EstadoOrden {

    PENDIENTE_PESAJE_INICIAL(1, "Pendiente de pesaje inicial"),
    CON_PESAJE_INICIAL(2, "Con pesaje inicial registrado"),
    CERRADA_PARA_CARGA(3, "Cerrada para carga"),
    FINALIZADA(4, "Finalizada");

    private final int numero;
    private final String descripcion;

    EstadoOrden(int numero, String descripcion) {
        this.numero = numero;
        this.descripcion = descripcion;
    }

    public int getNumero() {
        return numero;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /** True solo si {@code nuevo} es exactamente el estado siguiente (no se saltean ni se retroceden estados). */
    public boolean puedePasarA(EstadoOrden nuevo) {
        return nuevo != null && nuevo.numero == this.numero + 1;
    }
}
