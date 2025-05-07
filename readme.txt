README - File Upload JavaFX Client & Spring Boot Server

1. Requirements:
   - Java 17 or above
   - Maven (for backend)
   - JavaFX SDK (for frontend)
   - Internet connection (for libraries, if using Maven)

2. How to Run Backend (Spring Boot):
   - Navigate to the 'backend' folder
   - Run: mvn spring-boot:run
   - Server will start on port 8081

3. How to Run Frontend (JavaFX App):
   - Open the JavaFX project in IntelliJ or Eclipse
   - Make sure JavaFX libraries are configured
   - Run the main class (e.g., Main.java)

4. Functionality:
   - Upload a .txt file using the Import button
   - The content is sent to the server as JSON
   - The server logs the content and simulates document collaboration

5. Notes:
   - Files must be plain text (.txt)
   - Make sure the backend is running before using the frontend
