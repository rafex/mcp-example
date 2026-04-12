FROM nginx:alpine

# Copiar configuración de Nginx
COPY nginx.conf /etc/nginx/nginx.conf

# Crear directorio para documentación
RUN mkdir -p /usr/share/nginx/html

# Copiar documentación (si existe)
# COPY docs/ /usr/share/nginx/html/

EXPOSE 8085

CMD ["nginx", "-g", "daemon off;"]
