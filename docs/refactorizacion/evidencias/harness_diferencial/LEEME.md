# Prueba diferencial de la regla RN-03 (antes vs después)

`Diff.java` contiene una copia literal de `CitaService.validarHorario` tal como estaba **antes** del refactor
y la compara con `HorarioAtencion.validarCita` (**después**) sobre 500 pares inicio/fin generados al azar
(semilla fija 42). Los stubs de `@Value` y `@Component` evitan depender de Spring.

Ejecutar desde la raíz del proyecto:

```bash
D=docs/refactorizacion/evidencias/harness_diferencial
mkdir -p /tmp/h/out
javac -encoding UTF-8 -d /tmp/h/out \
  $(find $D/src -name "*.java") \
  src/main/java/com/vetcare/exception/BusinessException.java \
  src/main/java/com/vetcare/service/HorarioAtencion.java
java -Dstdout.encoding=UTF-8 -cp /tmp/h/out Diff
```

Resultado esperado: `diferencias=0` (ver `../resultado_diferencial.txt`).
