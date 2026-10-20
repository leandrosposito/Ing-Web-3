package ar.edu.iua.iw3.model;

/** Estados de la orden: solo se puede pasar al estado siguiente, nunca saltear ni retroceder. */
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


    public boolean puedePasarA(EstadoOrden nuevo) {
        return nuevo != null && nuevo.numero == this.numero + 1;
    }
}
