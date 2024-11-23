FROM node:23 AS build

# Set the working directory inside the container
WORKDIR /wild-tag

# Copy package.json and package-lock.json to the container
COPY /ui/package*.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application code to the container
COPY /ui ./

# Build the React application
RUN npm run build

# Stage 2: Serve the application with Nginx
FROM bitnami/nginx:latest

# Copy the built files from the previous stage
COPY --from=build /wild-tag/build /usr/share/nginx/html

# Copy the custom Nginx configuration if needed
# COPY nginx.conf /etc/nginx/nginx.conf

WORKDIR /usr/share/nginx/html