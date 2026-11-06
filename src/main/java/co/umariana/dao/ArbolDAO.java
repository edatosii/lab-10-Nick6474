package co.umariana.dao;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.Persona;
import co.umariana.modelo.ResumenArbol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ArbolDAO {

    public long guardar(String nombre, ArbolGenealogico arbol) {
        if (nombre == null || nombre.trim().isEmpty() || nombre.length() > 100 || arbol == null) {
            return -1;
        }

        String sqlArbol = "INSERT INTO arboles (nombre) VALUES (?)";
        String sqlMiembro = "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            long arbolId = -1;
            try (PreparedStatement psArbol = con.prepareStatement(sqlArbol, Statement.RETURN_GENERATED_KEYS)) {
                psArbol.setString(1, nombre.trim());
                int filasAfectadas = psArbol.executeUpdate();

                if (filasAfectadas == 0) {
                    con.rollback();
                    return -1;
                }

                try (ResultSet rsKeys = psArbol.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        arbolId = rsKeys.getLong(1);
                    } else {
                        con.rollback();
                        return -1;
                    }
                }
            }

            try (PreparedStatement psMiembro = con.prepareStatement(sqlMiembro)) {
                List<Persona> miembros = arbol.miembros();
                for (Persona p : miembros) {
                    psMiembro.setLong(1, arbolId);
                    psMiembro.setString(2, p.getPosicion());
                    psMiembro.setString(3, p.getNombre());
                    psMiembro.setInt(4, p.getNacimiento());

                    if (p.getFallecimiento() != null) {
                        psMiembro.setInt(5, p.getFallecimiento());
                    } else {
                        psMiembro.setNull(5, java.sql.Types.INTEGER);
                    }
                    psMiembro.executeUpdate();
                }
            }

            con.commit();
            con.setAutoCommit(true);
            return arbolId;

        } catch (SQLException e) {
            System.out.println("Error al guardar árbol: " + e.getMessage());
            return -1;
        }
    }

    public List<ResumenArbol> listar() {
        List<ResumenArbol> resumenes = new ArrayList<>();
        String sql = "SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total " +
                     "FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id " +
                     "GROUP BY a.id, a.nombre, a.creado ORDER BY a.id";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ResumenArbol resumen = new ResumenArbol(
                        rs.getLong("id"),
                        rs.getString("nombre"),
                        rs.getInt("total"),
                        rs.getTimestamp("creado").toLocalDateTime()
                );
                resumenes.add(resumen);
            }

        } catch (SQLException e) {
            System.out.println("Error al listar árboles: " + e.getMessage());
        }

        return resumenes;
    }

    public ArbolGenealogico cargar(long id) {
        String sql = "SELECT posicion, nombre, nacimiento, fallecimiento FROM miembros WHERE arbol_id = ? ORDER BY id";
        ArbolGenealogico arbol = null;

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setLong(1, id);
            
            try (ResultSet rs = ps.executeQuery()) {
                boolean primerFila = true;
                
                while (rs.next()) {
                    String posicion = rs.getString("posicion");
                    String nombre = rs.getString("nombre");
                    int nacimiento = rs.getInt("nacimiento");
                    
                    int f = rs.getInt("fallecimiento");
                    Integer fallecimiento = rs.wasNull() ? null : f;
                    
                    Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
                    
                    if (primerFila) {
                        arbol = new ArbolGenealogico(persona);
                        primerFila = false;
                    } else {
                        if (arbol != null) {
                            arbol.agregar(persona);
                        }
                    }
                }
            }
            
        } catch (SQLException e) {
            System.out.println("Error al cargar árbol: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("Error reconstruyendo el árbol: " + e.getMessage());
            return null;
        }
        
        return arbol;
    }

    public boolean actualizar(long id, ArbolGenealogico arbol) {
        if (arbol == null) {
            return false;
        }

        String sqlCheck = "SELECT id FROM arboles WHERE id = ?";
        String sqlDelete = "DELETE FROM miembros WHERE arbol_id = ?";
        String sqlInsert = "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";

        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);

            try (PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
                psCheck.setLong(1, id);
                try (ResultSet rsCheck = psCheck.executeQuery()) {
                    if (!rsCheck.next()) {
                        con.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement psDelete = con.prepareStatement(sqlDelete)) {
                psDelete.setLong(1, id);
                psDelete.executeUpdate();
            }

            try (PreparedStatement psInsert = con.prepareStatement(sqlInsert)) {
                List<Persona> miembros = arbol.miembros();
                for (Persona p : miembros) {
                    psInsert.setLong(1, id);
                    psInsert.setString(2, p.getPosicion());
                    psInsert.setString(3, p.getNombre());
                    psInsert.setInt(4, p.getNacimiento());

                    if (p.getFallecimiento() != null) {
                        psInsert.setInt(5, p.getFallecimiento());
                    } else {
                        psInsert.setNull(5, java.sql.Types.INTEGER);
                    }
                    psInsert.executeUpdate();
                }
            }

            con.commit();
            con.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            System.out.println("Error al actualizar árbol: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(long id) {
        String sql = "DELETE FROM arboles WHERE id = ?";

        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.out.println("Error al eliminar árbol: " + e.getMessage());
            return false;
        }
    }
}