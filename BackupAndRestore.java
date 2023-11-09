import java.io.*;
import java.util.Scanner;
import java.util.zip.CRC32;

public class BackupAndRestore {
    public static void main(String[] args) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        boolean exit = false;

        while (!exit) {
            try {
                System.out.println("\n                      <<<<< ----------------------------------------- >>>>>                      ");
                System.out.println("                                        Choose an option                       ");
                System.out.println("                                      --------------------                          ");
                System.out.println("                                      -  1.Backup file   -  ");
                System.out.println("                                      -  2.Restore file  -    ");
                System.out.println("                                      -  3.Exit          - ");
                System.out.println("                                      --------------------                          ");
               // System.out.println("-----------------------------------------");

                int option = Integer.parseInt(reader.readLine());

                switch (option) {
                    case 1:
                        System.out.print("Enter the source file path: ");
                        String sourceFilePath = removeQuotes(reader.readLine());

                        System.out.print("Enter the backup folder path: ");
                        String backupFolderPath = removeQuotes(reader.readLine());

                        backupFile(sourceFilePath, backupFolderPath);
                        break;
                    case 2:
                        System.out.print("Enter the source file path: ");
                        sourceFilePath = removeQuotes(reader.readLine());

                        System.out.print("Enter the backup folder path: ");
                        backupFolderPath = removeQuotes(reader.readLine());

                        // System.out.print("Enter the corrupted folder path: ");
                        // String corruptedFolderPath = removeQuotes(reader.readLine());
                        String corruptedFolderPath = "D:\\fs_mini_project\\corruptfolder";


                        restoreFile(sourceFilePath, backupFolderPath, corruptedFolderPath);
                        break;
                    case 3:
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




    public static void backupFile(String sourceFilePath, String backupFolderPath) {
        // Check if the source file exists
        File sourceFile = new File(sourceFilePath);
        if (!sourceFile.exists()) {
            System.out.println("\nError: Source file not found.");
            return;
        }
        // Check if the backup folder exists, create it if necessary
        File backupFolder = new File(backupFolderPath);
        if (!backupFolder.exists()) {
            if (!backupFolder.mkdirs()) {
                System.out.println("\nError: Failed to create the backup folder.");
                return;
            }
        }
        // Generate the backup file path
        String sourceFileName = sourceFile.getName();
        String backupFilePath = backupFolderPath + File.separator + sourceFileName + ".bak";
        // Compress the file using run-length encoding
        boolean isCompressed = compressRLE(sourceFilePath, backupFilePath);
        if (isCompressed) {
            System.out.println("\nBackup created: --->  " + backupFilePath);
        } else {
            System.out.println("\nError: Failed to create the backup.");
        }
    }






  public static void restoreFile(String sourceFilePath, String backupFolderPath, String corruptedFolderPath) {
    // Convert the source file to .txt format
    String txtSourceFilePath = sourceFilePath.substring(0, sourceFilePath.lastIndexOf(".")) + ".txt";
    File txtSourceFile = new File(txtSourceFilePath);

    if (sourceFilePath.endsWith(".txt")) {
        // The source file is already in .txt format, no need for conversion
        txtSourceFile = new File(sourceFilePath);
    } else {
        // Convert the source file to .txt format
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(txtSourceFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
            System.out.println("Source file converted to .txt format: ---> " + txtSourceFilePath);
        } catch (IOException e) {
            System.out.println("Error: Failed to convert source file to .txt format.");
            return;
        }
    }

    // Generate the backup file path
    String sourceFileName = txtSourceFile.getName();
    String backupFilePath = backupFolderPath + File.separator + sourceFileName + ".bak";

    // Check if the backup file exists
    File backupFile = new File(backupFilePath);
    if (!backupFile.exists()) {
        System.out.println("\nError: Backup file not found in the given folder.");
        return;
    }

    // Decompress the backup file
    String decompressedFilePath = backupFolderPath + File.separator + "decompressed_" + sourceFileName;
    boolean isDecompressed = decompressRLE(backupFilePath, decompressedFilePath);

    if (isDecompressed) {
        // Verify the integrity of the decompressed file
        boolean isIntegrityVerified = checkFileIntegrity(txtSourceFilePath, decompressedFilePath);

        if (isIntegrityVerified) {
            System.out.println("\n**********************\nSource file is not corrupted. \nNo changes needed.\n*********************\n");
        } else {
            // Move the corrupted source file to the corrupted folder
            File sourceFile = new File(txtSourceFilePath);
            File corruptedFolder = new File(corruptedFolderPath);
            File corruptedFile = new File(corruptedFolder, sourceFileName);

            // Only proceed if the integrity check detected an error
            if (!corruptedFile.exists() || replaceCorruptedFile(corruptedFile, sourceFile)) {
                System.out.println("\n******************\nSource file is corrupted. \nChanges needed.\n******************\n");

                if (corruptedFile.exists() && !corruptedFile.delete()) {
                    System.out.println("\nError: Failed to replace the existing corrupted file.");
                    return;
                }

                if (sourceFile.renameTo(corruptedFile)) {
                    System.out.println("Source file moved to corrupted folder: ---> " + corruptedFile.getAbsolutePath());

                    // Replace the source file with the decompressed backup file
                    File decompressedFile = new File(decompressedFilePath);

                    if (decompressedFile.renameTo(sourceFile)) {
                        System.out.println("File restored: ---> " + txtSourceFilePath);
                    } else {
                        System.out.println("\nError: Failed to restore file.");
                    }
                } else {
                    System.out.println("\nError: Failed to move source file to corrupted folder.");
                }
            } else {
                // Replace the source file with the decompressed backup file
                File decompressedFile = new File(decompressedFilePath);

                if (decompressedFile.renameTo(sourceFile)) {
                    System.out.println("File restored: ---> " + txtSourceFilePath);
                } else {
                    System.out.println("\nError: Failed to restore file.");
                }
            }
        }
    } else {
        System.out.println("\nError: Failed to decompress backup file.");
    }

    // Delete the source file if it exists (only for non-txt files)
    if (!txtSourceFile.getName().equals(sourceFileName)) {
        if (!txtSourceFile.delete()) {
            System.out.println("\nError: Failed to delete the source file.");
        }
    }
}







    private static boolean replaceCorruptedFile(File corruptedFile, File sourceFile) {
        System.out.println("\n<<< Corrupted file already exists in the corrupted folder and is replaced >>>");
        return true; // Replace the corrupted file directly without asking the user
    }




    public static boolean compressRLE(String sourceFilePath, String compressedFilePath) {
        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(compressedFilePath));
             BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath))) {

            int prevChar = reader.read();
            int count = 1;
            int currentChar;

            while ((currentChar = reader.read()) != -1) {
                if (currentChar == prevChar) {
                    count++;
                } else {
                    output.writeChar(prevChar);
                    output.writeInt(count);
                    prevChar = currentChar;
                    count = 1;
                }
            }

            output.writeChar(prevChar);
            output.writeInt(count);

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    public static boolean decompressRLE(String compressedFilePath, String decompressedFilePath) {
        try (DataInputStream input = new DataInputStream(new FileInputStream(compressedFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(decompressedFilePath))) {

            while (input.available() > 0) {
                char character = input.readChar();
                int count = input.readInt();

                for (int i = 0; i < count; i++) {
                    writer.write(character);
                }
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    public static boolean checkFileIntegrity(String sourceFilePath, String decompressedFilePath) {
        try (FileInputStream sourceFileInput = new FileInputStream(sourceFilePath);
             FileInputStream decompressedFileInput = new FileInputStream(decompressedFilePath)) {
            CRC32 sourceCRC32 = new CRC32();
            CRC32 decompressedCRC32 = new CRC32();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = sourceFileInput.read(buffer)) != -1) {
                sourceCRC32.update(buffer, 0, bytesRead);
            }
            while ((bytesRead = decompressedFileInput.read(buffer)) != -1) {
                decompressedCRC32.update(buffer, 0, bytesRead);
            }
            return sourceCRC32.getValue() == decompressedCRC32.getValue();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    private static String removeQuotes(String path)
    {
        if (path.startsWith("\"") && path.endsWith("\"")) {
            return path.substring(1, path.length() - 1);
        }
     return path;
    }

}
