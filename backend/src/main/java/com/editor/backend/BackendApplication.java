package com.editor.backend;

import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.editor.backend.service.CRDTService;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	CommandLineRunner runTest(CRDTService crdt) {
		return args -> {
			Scanner scanner = new Scanner(System.in);
			System.out.println("🔠 CRDT Terminal Test Started");
			System.out.println("Commands:");
			System.out.println(" insert <char> <parentId> <userId> <clock>");
			System.out.println(" delete <nodeId>");
			System.out.println(" undo <userId>");
			System.out.println(" redo <userId>");
			System.out.println(" print");
			System.out.println(" exit");

			while (true) {
				System.out.print("> ");
				String line = scanner.nextLine();
				if (line.equalsIgnoreCase("exit")) break;

				String[] parts = line.trim().split(" ");
				if (parts.length == 0) continue;

				try {
					switch (parts[0].toLowerCase()) {
						case "insert":
							if (parts.length == 5) {
								char value = parts[1].charAt(0);
								String parentId = parts[2];
								String userId = parts[3];
								long clock = Long.parseLong(parts[4]);
								crdt.insert(value, parentId, userId, clock);
							} else {
								System.out.println("❌ Usage: insert <char> <parentId> <userId> <clock>");
							}
							break;

						case "delete":
							if (parts.length == 2) {
								crdt.delete(parts[1]);
							} else {
								System.out.println("❌ Usage: delete <nodeId>");
							}
							break;

						case "undo":
							if (parts.length == 2) {
								crdt.undo(parts[1]);
							} else {
								System.out.println("❌ Usage: undo <userId>");
							}
							break;

						case "redo":
							if (parts.length == 2) {
								crdt.redo(parts[1]);
							} else {
								System.out.println("❌ Usage: redo <userId>");
							}
							break;

						case "print":
							System.out.println("📄 Document: " + crdt.getDocument());
							break;

						default:
							System.out.println("❌ Unknown command. Type 'print' or 'exit'.");
					}
				} catch (Exception e) {
					System.out.println("⚠️ Error: " + e.getMessage());
				}
			}
		};
	}
}
