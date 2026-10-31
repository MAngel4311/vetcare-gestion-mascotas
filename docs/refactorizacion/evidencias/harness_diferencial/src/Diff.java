import com.vetcare.exception.BusinessException;
import com.vetcare.service.HorarioAtencion;
import java.time.*; import java.util.*;
public class Diff {
  // ANTES: copia literal de CitaService.validarHorario original (versión previa al refactor)
  static String apertura="08:00", cierre="18:00";
  static void antes(LocalDateTime inicio, LocalDateTime fin, LocalDateTime ahora) {
    if (!fin.isAfter(inicio)) throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio");
    if (!inicio.isAfter(ahora)) throw new BusinessException("La cita debe programarse en una fecha y hora futuras");
    if (!inicio.toLocalDate().equals(fin.toLocalDate())) throw new BusinessException("La cita debe empezar y terminar el mismo día");
    if (inicio.getDayOfWeek() == DayOfWeek.SUNDAY) throw new BusinessException("La clínica no atiende los domingos (horario: lunes a sábado)");
    LocalTime abre = LocalTime.parse(apertura), cierra = LocalTime.parse(cierre);
    if (inicio.toLocalTime().isBefore(abre) || fin.toLocalTime().isAfter(cierra))
      throw new BusinessException("La cita debe estar dentro del horario de atención (" + abre + " a " + cierra + ", lunes a sábado)");
  }
  static String run(Runnable r){ try { r.run(); return "OK"; } catch (BusinessException e){ return e.getMessage(); } }
  public static void main(String[] a){
    HorarioAtencion h = new HorarioAtencion("08:00","18:00");
    Random rnd = new Random(42); int n=500, diff=0; Map<String,Integer> dist=new TreeMap<>();
    LocalDateTime base = LocalDateTime.of(2026,10,1,0,0);
    for(int i=0;i<n;i++){
      LocalDateTime ahora = base.plusMinutes(rnd.nextInt(60*24*40));
      LocalDateTime ini = base.plusMinutes(rnd.nextInt(60*24*45));
      LocalDateTime fin = rnd.nextInt(10)==0 ? ini.plusMinutes(rnd.nextInt(60*30)-60) : ini.plusMinutes(15+rnd.nextInt(180));
      final LocalDateTime A=ahora,I=ini,F=fin;
      String r1=run(()->antes(I,F,A)), r2=run(()->h.validarCita(I,F,A));
      dist.merge(r1,1,Integer::sum);
      if(!r1.equals(r2)){ diff++; if(diff<5) System.out.println("DIF "+I+" "+F+" "+A+" -> "+r1+" | "+r2); }
    }
    System.out.println("casos="+n+" diferencias="+diff);
    dist.forEach((k,v)->System.out.println(String.format("%7d  %s",v,k)));
    try { new HorarioAtencion("18:00","08:00"); System.out.println("config invalida: NO falla (mal)"); }
    catch(IllegalStateException e){ System.out.println("config invalida: falla al arrancar (OK)"); }
  }
}
