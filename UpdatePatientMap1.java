import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AnonymisePatientData {

    public static void main(String[] args) {
        String inputFilePath = "PatientNotes.txt";
        String anonymizedFile = "AnonymisedMedicalNotes.txt";
        String mappingFile = "MappingDocument.txt";

        try {
            List<String> lines = Files.readAllLines(Paths.get(inputFilePath), StandardCharsets.UTF_8);
            List<String> anonymizedLines = new ArrayList<>();
            Set<String> usedReplacements = new HashSet<>();
            Map<String, String> nameToKeyMap = new HashMap<>();
            List<String> mappingLines = new ArrayList<>();
            int patientCounter = 1;

            Pattern pattern = Pattern.compile(
                    // Full name
                    "(Mr\\.|Mrs\\.|Ms\\.|Miss|Dr\\.|Prof\\.|Master|Rev\\.|Fr\\.|Sr\\.|Smt\\.|Mx\\.|Lady|Sir|Capt\\.|Major|Col\\.|Lt\\.|Hon\\.|Judge)\\s+([A-Z][a-z]+)\\s+([A-Z][a-z]+)|" +
                    // Title + first name
                    "(Mr\\.|Mrs\\.|Ms\\.|Miss|Dr\\.|Prof\\.|Master|Rev\\.|Fr\\.|Sr\\.|Smt\\.|Mx\\.|Lady|Sir|Capt\\.|Major|Col\\.|Lt\\.|Hon\\.|Judge)\\s+([A-Z][a-z]+)|" +
                    // Age
                    "(\\d+)-year-old|aged\\s+(\\d+)|" +
                    // Address
                    "(at\\s+|in\\s+|of\\s+|residing\\s+|resident of\\s+|living at\\s+|residing in\\s+|residing at\\s+|chilling at\\s+)" +
                    "(((\\d+\\s)?[A-Za-z0-9.]+\\s)*(Street|Avenue|Road|Lane|Drive|Boulevard|Terrace|Court),\\s+[A-Za-z\\s]+,\\s+[A-Z]{2}|[A-Za-z\\s]+,\\s+[A-Z]{2})|" +
                    // DOB
                    "(\\b\\d{1,2}(st|nd|rd|th)?\\s+(January|February|March|April|May|June|July|August|September|October|November|December),?\\s+\\d{4}\\b)|" +
                    "(\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2}(st|nd|rd|th)?,?\\s+\\d{4}\\b)"
            );

            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);
                StringBuffer sb = new StringBuffer();
                int infoCounter = 1;

                while (matcher.find()) {
                    String replacement = "";

                    if (matcher.group(1) != null) { // Full name
                        String title = matcher.group(1);
                        String firstName = matcher.group(2);
                        String lastName = matcher.group(3);
                        String fullName = title + " " + firstName + " " + lastName;

                        if (nameToKeyMap.containsKey(fullName)) {
                            replacement = nameToKeyMap.get(fullName);
                        } else {
                            replacement = patientCounter + "." + infoCounter;
                            nameToKeyMap.put(fullName, replacement);
                            nameToKeyMap.put(title + " " + firstName, replacement);
                            nameToKeyMap.put(firstName + " " + lastName, replacement);
                            nameToKeyMap.put(firstName, replacement);
                            nameToKeyMap.put(lastName, replacement);
                            if (!usedReplacements.contains(replacement)) {
                                mappingLines.add(replacement + "\t" + fullName);
                                usedReplacements.add(replacement);
                                infoCounter++;
                            }
                        }
                    } else if (matcher.group(4) != null) { // Title + First Name only
                        String title = matcher.group(4);
                        String firstName = matcher.group(5);
                        String shortName = title + " " + firstName;

                        if (nameToKeyMap.containsKey(shortName)) {
                            replacement = nameToKeyMap.get(shortName);
                        } else {
                            replacement = patientCounter + "." + infoCounter;
                            nameToKeyMap.put(shortName, replacement);
                            nameToKeyMap.put(firstName, replacement);
                            if (!usedReplacements.contains(replacement)) {
                                mappingLines.add(replacement + "\t" + shortName);
                                usedReplacements.add(replacement);
                                infoCounter++;
                            }
                        }
                    } else if (matcher.group(6) != null || matcher.group(7) != null) { // Age
                        String age = matcher.group(6) != null ? matcher.group(6) : matcher.group(7);
                        String key = patientCounter + "." + infoCounter ;
                        String originalAgeText = matcher.group(6) != null ? age + "-year-old" : "aged " + age;
                        if (!usedReplacements.contains(key)) {
                            mappingLines.add(key + "\t" + originalAgeText);
                            usedReplacements.add(key);
                        }
                        replacement = key;
                        infoCounter++;
                    } else if (matcher.group(8) != null) { // Address
                        String key = matcher.group(8) + patientCounter + "." + infoCounter;
                        String address = matcher.group(9).trim();
                        if (!usedReplacements.contains(key)) {
                            mappingLines.add(patientCounter + "." + infoCounter + "\t" + address);
                            usedReplacements.add(key);
                        }
                        replacement = key;
                        infoCounter++;
                    } else if (matcher.group(15) != null || matcher.group(19) != null) { // DOB
                        String dob = matcher.group(); // full match
                        String key = patientCounter + "." + infoCounter ;
                        if (!usedReplacements.contains(key)) {
                            mappingLines.add(key + "\t" + dob);
                            usedReplacements.add(key);
                        }
                        replacement = key;
                        infoCounter++;
                    }

                    matcher.appendReplacement(sb, replacement);
                }

                matcher.appendTail(sb);

                // Replace any remaining standalone name parts with their mapped keys
                String anonymizedText = sb.toString();
                for (Map.Entry<String, String> entry : nameToKeyMap.entrySet()) {
                    anonymizedText = anonymizedText.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
                }

                anonymizedLines.add(anonymizedText);
                patientCounter++;
            }

            Files.write(Paths.get(anonymizedFile), anonymizedLines, StandardCharsets.UTF_8);
            Files.write(Paths.get(mappingFile), mappingLines, StandardCharsets.UTF_8);

            System.out.println("Patient-Data File anonymization completed successfully! and Mapping document created. ");
        } catch (IOException e) {
            System.err.println("Currnt Code has Read/write Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
