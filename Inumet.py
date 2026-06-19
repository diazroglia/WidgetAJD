import urllib.request
import json

url = "https://www.inumet.gub.uy/reportes/estadoActual/datos_inumet_ui_publica.mch"
req = urllib.request.Request(url, headers={'Accept': 'application/json', 'User-Agent': 'Mozilla/5.0'})

try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
        
        print("=== VARIABLES DISPONIBLES EN INUMET ===")
        for var in data.get('variables', []):
            print(f"ID: {var.get('id')}, Nombre: {var.get('nombre')}, Unidad: {var.get('unidad')}")
            
        print("\n=== EJEMPLO DE DATOS DE ESTACION DE MONTEVIDEO ===")
        # Prado station ID is typically 211 or we can find it
        prado_idx = None
        for i, est in enumerate(data.get('estaciones', [])):
            if "prado" in est.get('nombre', '').lower() or est.get('id') == 211:
                prado_idx = i
                print(f"Estación: {est.get('nombre')} (Indice: {i}, ID: {est.get('id')})")
                break
                
        if prado_idx is not None:
            for obs in data.get('observaciones', []):
                var_info = obs.get('variable', {})
                var_name = var_info.get('nombre')
                var_id = var_info.get('id')
                datos_est = obs.get('datos', [])
                val = datos_est[prado_idx] if prado_idx < len(datos_est) else None
                print(f"  - {var_name} (ID {var_id}): {val}")
except Exception as e:
    print(f"Error: {e}")