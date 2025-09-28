import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.*;
public class AstronautScheduleOrganizer {

    // ---------- Logging ----------
    private static final Logger LOGGER = Logger.getLogger(AstronautScheduleOrganizer.class.getName());
    static {
        // Simple console logger setup
        Logger root = Logger.getLogger("");
        Handler[] handlers = root.getHandlers();
        for (Handler h : handlers) {
            if (h instanceof ConsoleHandler) {
                h.setLevel(Level.INFO);
            }
        }
        LOGGER.setLevel(Level.INFO);
    }

    // ---------- Entry Point ----------
    public static void main(String[] args) {
        System.out.println("=== Astronaut Daily Schedule Organizer ===");
        System.out.println("Type 'help' to see commands.\n");

        ScheduleManager manager = ScheduleManager.getInstance();
        // Attach a simple observer for conflict/updates notifications
        manager.addObserver(new ConflictNotifier());

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;
            String[] tokens = line.split("\\s+", 2);
            String cmd = tokens[0].toLowerCase();

            try {
                switch (cmd) {
                    case "help":
                        printHelp();
                        break;
                    case "add":
                        // Format: add "Description" HH:mm HH:mm Priority
                        if (tokens.length < 2) {
                            System.out.println("Usage: add \"Description\" HH:mm HH:mm Priority");
                            break;
                        }
                        handleAdd(tokens[1], manager);
                        break;
                    case "remove":
                        // remove "Description"
                        if (tokens.length < 2) {
                            System.out.println("Usage: remove \"Description\"");
                            break;
                        }
                        handleRemove(tokens[1], manager);
                        break;
                    case "view":
                        // view OR view all OR view priority High/Medium/Low
                        if (tokens.length == 1 || tokens[1].trim().equalsIgnoreCase("all")) {
                            manager.viewAllTasks();
                        } else if (tokens[1].toLowerCase().startsWith("priority")) {
                            String[] p = tokens[1].split("\\s+");
                            if (p.length < 2) { System.out.println("Usage: view priority High|Medium|Low"); break; }
                            manager.viewByPriority(Priority.fromString(p[1]));
                        } else {
                            System.out.println("Invalid view command. Use 'view' or 'view priority <level>'.");
                        }
                        break;
                    case "edit":
                        // edit "OldDescription" "NewDescription" HH:mm HH:mm Priority
                        if (tokens.length < 2) {
                            System.out.println("Usage: edit \"OldDescription\" \"NewDescription\" HH:mm HH:mm Priority");
                            break;
                        }
                        handleEdit(tokens[1], manager);
                        break;
                    case "complete":
                        // complete "Description"
                        if (tokens.length < 2) {
                            System.out.println("Usage: complete \"Description\"");
                            break;
                        }
                        handleComplete(tokens[1], manager);
                        break;
                    case "exit":
                    case "quit":
                        running = false;
                        System.out.println("Exiting. Goodbye!");
                        break;
                    default:
                        System.out.println("Unknown command. Type 'help' for a list of commands.");
                }
            } catch (ScheduleException se) {
                System.out.println("Error: " + se.getMessage());
                LOGGER.warning(se.getMessage());
            } catch (Exception e) {
                System.out.println("An unexpected error occurred: " + e.getMessage());
                LOGGER.log(Level.SEVERE, "Unexpected error", e);
            }
        }

        scanner.close();
    }

    // ---------- Input Handlers ----------
    private static void handleAdd(String args, ScheduleManager manager) throws ScheduleException {
        // Expecting: "Description" HH:mm HH:mm Priority
        ParsedAdd parsed = parseAddArgs(args);
        Task t = TaskFactory.createTask(parsed.description, parsed.start, parsed.end, parsed.priority);
        manager.addTask(t);
    }

    private static void handleRemove(String args, ScheduleManager manager) throws ScheduleException {
        String desc = extractQuoted(args);
        if (desc == null) throw new ScheduleException("Provide description enclosed in quotes: \"Description\"");
        manager.removeTaskByDescription(desc);
    }

    private static void handleEdit(String args, ScheduleManager manager) throws ScheduleException {
        // "OldDescription" "NewDescription" HH:mm HH:mm Priority
        String[] parts = splitByQuoted(args);
        if (parts.length < 2) throw new ScheduleException("Usage: edit \"OldDescription\" \"NewDescription\" HH:mm HH:mm Priority");
        String oldDesc = parts[0];
        String remainder = parts[1].trim();
        ParsedAdd parsed = parseAddArgs(remainder);
        manager.editTask(oldDesc, parsed.description, parsed.start, parsed.end, parsed.priority);
    }

    private static void handleComplete(String args, ScheduleManager manager) throws ScheduleException {
        String desc = extractQuoted(args);
        if (desc == null) throw new ScheduleException("Provide description enclosed in quotes: \"Description\"");
        manager.markCompleted(desc);
    }

    // ---------- Utilities for parsing ----------
    private static class ParsedAdd {
        String description;
        LocalTime start;
        LocalTime end;
        Priority priority;
    }

    private static ParsedAdd parseAddArgs(String args) throws ScheduleException {
        // args may start with "Description"
        String desc = extractQuoted(args);
        if (desc == null) throw new ScheduleException("Provide description enclosed in quotes: \"Description\"");
        String remainder = args.substring(args.indexOf("\"", args.indexOf("\"") + 1) + 1).trim();
        String[] tokens = remainder.split("\\s+");
        if (tokens.length < 3) throw new ScheduleException("Provide startTime endTime priority. Example: 07:00 08:00 High");
        String sTime = tokens[0], eTime = tokens[1], pr = tokens[2];
        LocalTime start = parseTime(sTime);
        LocalTime end = parseTime(eTime);
        if (!end.isAfter(start) && !end.equals(start)) throw new ScheduleException("End time must be after start time.");
        ParsedAdd p = new ParsedAdd();
        p.description = desc;
        p.start = start;
        p.end = end;
        p.priority = Priority.fromString(pr);
        return p;
    }

    private static LocalTime parseTime(String t) throws ScheduleException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("H:mm");
        try {
            return LocalTime.parse(t, fmt);
        } catch (DateTimeParseException dte) {
            throw new ScheduleException("Invalid time format. Use HH:mm (e.g., 07:30).");
        }
    }

    // Extract quoted string first occurrence
    private static String extractQuoted(String s) {
        int first = s.indexOf('"');
        if (first < 0) return null;
        int second = s.indexOf('"', first + 1);
        if (second < 0) return null;
        return s.substring(first + 1, second);
    }

    // Split two quoted parts: returns array [firstQuoted, remainderAfterSecondQuote]
    private static String[] splitByQuoted(String s) throws ScheduleException {
        int first = s.indexOf('"');
        if (first < 0) throw new ScheduleException("Missing quoted section.");
        int second = s.indexOf('"', first + 1);
        if (second < 0) throw new ScheduleException("Missing closing quote.");
        String quoted = s.substring(first + 1, second);
        String remainder = s.substring(second + 1).trim();
        return new String[]{quoted, remainder};
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  add \"Description\" HH:mm HH:mm Priority    - Add task (Priority: High|Medium|Low)");
        System.out.println("  remove \"Description\"                      - Remove a task by description");
        System.out.println("  edit \"OldDescription\" \"NewDescription\" HH:mm HH:mm Priority - Edit a task");
        System.out.println("  complete \"Description\"                    - Mark a task completed");
        System.out.println("  view                                       - View all tasks (sorted by start time)");
        System.out.println("  view all                                   (same as view)");
        System.out.println("  view priority High|Medium|Low              - View tasks filtered by priority");
        System.out.println("  help                                       - Show this help");
        System.out.println("  exit                                       - Quit");
    }

    // ---------- Domain Classes & Factories & Patterns ----------

    enum Priority {
        HIGH, MEDIUM, LOW;

        public static Priority fromString(String s) throws ScheduleException {
            if (s == null) throw new ScheduleException("Priority must be High, Medium or Low.");
            s = s.trim().toUpperCase();
            switch (s) {
                case "HIGH": return HIGH;
                case "MEDIUM": return MEDIUM;
                case "LOW": return LOW;
                default: throw new ScheduleException("Invalid priority. Use High, Medium or Low.");
            }
        }
    }

    static class Task implements Comparable<Task> {
        private final UUID id;
        private String description;
        private LocalTime start;
        private LocalTime end;
        private Priority priority;
        private boolean completed;

        Task(String description, LocalTime start, LocalTime end, Priority priority) {
            this.id = UUID.randomUUID();
            this.description = description;
            this.start = start;
            this.end = end;
            this.priority = priority;
            this.completed = false;
        }

        public UUID getId() { return id; }
        public String getDescription() { return description; }
        public LocalTime getStart() { return start; }
        public LocalTime getEnd() { return end; }
        public Priority getPriority() { return priority; }
        public boolean isCompleted() { return completed; }

        public void setDescription(String d) { this.description = d; }
        public void setStart(LocalTime s) { this.start = s; }
        public void setEnd(LocalTime e) { this.end = e; }
        public void setPriority(Priority p) { this.priority = p; }
        public void markCompleted() { this.completed = true; }

        @Override
        public int compareTo(Task other) {
            int cmp = this.start.compareTo(other.start);
            if (cmp != 0) return cmp;
            cmp = this.end.compareTo(other.end);
            if (cmp != 0) return cmp;
            return this.id.compareTo(other.id);
        }

        @Override
        public String toString() {
            String done = completed ? " [COMPLETED]" : "";
            return String.format("%s - %s: %s [%s]%s",
                    start.toString(), end.toString(), description, priority, done);
        }
    }

    // Factory Pattern
    static class TaskFactory {
        public static Task createTask(String description, LocalTime start, LocalTime end, Priority priority) {
            return new Task(description, start, end, priority);
        }
    }

    // Observer Pattern
    interface ScheduleObserver {
        void onConflict(Task newTask, Task conflictingTask);
        void onTaskAdded(Task t);
        void onTaskRemoved(Task t);
        void onTaskEdited(Task oldTask, Task newTask);
        void onTaskCompleted(Task t);
    }

    static class ConflictNotifier implements ScheduleObserver {
        @Override
        public void onConflict(Task newTask, Task conflictingTask) {
            System.out.printf("Conflict: \"%s\" conflicts with existing task \"%s\" (%s - %s).\n",
                    newTask.getDescription(), conflictingTask.getDescription(),
                    conflictingTask.getStart(), conflictingTask.getEnd());
        }

        @Override
        public void onTaskAdded(Task t) {
            System.out.printf("Task added: %s - %s: \"%s\" [%s]\n", t.getStart(), t.getEnd(), t.getDescription(), t.getPriority());
        }

        @Override
        public void onTaskRemoved(Task t) {
            System.out.printf("Task removed: \"%s\"\n", t.getDescription());
        }

        @Override
        public void onTaskEdited(Task oldTask, Task newTask) {
            System.out.printf("Task edited: \"%s\" -> \"%s\"\n", oldTask.getDescription(), newTask.getDescription());
        }

        @Override
        public void onTaskCompleted(Task t) {
            System.out.printf("Task completed: \"%s\"\n", t.getDescription());
        }
    }

    // Custom Exception for schedule operations
    static class ScheduleException extends Exception {
        ScheduleException(String message) { super(message); }
    }

    // Singleton Pattern: ScheduleManager
    static class ScheduleManager {
        private static ScheduleManager instance;
        private final NavigableSet<Task> tasksByTime;
        private final Map<String, Task> tasksByDescriptionLower;
        private final List<ScheduleObserver> observers;

        private ScheduleManager() {
            tasksByTime = new TreeSet<>();
            tasksByDescriptionLower = new HashMap<>();
            observers = new ArrayList<>();
        }

        public static synchronized ScheduleManager getInstance() {
            if (instance == null) {
                instance = new ScheduleManager();
            }
            return instance;
        }

        public void addObserver(ScheduleObserver o) {
            observers.add(o);
        }

        public void removeObserver(ScheduleObserver o) {
            observers.remove(o);
        }

        // Add task with conflict check
        public synchronized void addTask(Task t) throws ScheduleException {
            Objects.requireNonNull(t, "Task cannot be null");
            validateNoOverlap(t, null);
            tasksByTime.add(t);
            tasksByDescriptionLower.put(t.getDescription().toLowerCase(), t);
            LOGGER.info("Task added: " + t.getDescription());
            observers.forEach(o -> o.onTaskAdded(t));
        }

        // Remove by description
        public synchronized void removeTaskByDescription(String desc) throws ScheduleException {
            Task t = tasksByDescriptionLower.get(desc.toLowerCase());
            if (t == null) throw new ScheduleException("Task not found: " + desc);
            tasksByTime.remove(t);
            tasksByDescriptionLower.remove(desc.toLowerCase());
            LOGGER.info("Task removed: " + desc);
            observers.forEach(o -> o.onTaskRemoved(t));
        }

        // Edit: find by oldDesc, then apply new fields (must re-check overlap)
        public synchronized void editTask(String oldDesc, String newDesc, LocalTime newStart, LocalTime newEnd, Priority newPriority) throws ScheduleException {
            Task existing = tasksByDescriptionLower.get(oldDesc.toLowerCase());
            if (existing == null) throw new ScheduleException("Task not found: " + oldDesc);

            // Create temporary task for validation (exclude existing task from overlap check)
            Task temp = TaskFactory.createTask(newDesc, newStart, newEnd, newPriority);
            validateNoOverlap(temp, existing);

            // Remove then re-add with updated values
            tasksByTime.remove(existing);
            tasksByDescriptionLower.remove(oldDesc.toLowerCase());

            existing.setDescription(newDesc);
            existing.setStart(newStart);
            existing.setEnd(newEnd);
            existing.setPriority(newPriority);

            tasksByTime.add(existing);
            tasksByDescriptionLower.put(newDesc.toLowerCase(), existing);

            LOGGER.info("Task edited: " + oldDesc + " -> " + newDesc);
            observers.forEach(o -> o.onTaskEdited(existing, existing));
        }

        public synchronized void markCompleted(String desc) throws ScheduleException {
            Task t = tasksByDescriptionLower.get(desc.toLowerCase());
            if (t == null) throw new ScheduleException("Task not found: " + desc);
            t.markCompleted();
            LOGGER.info("Task completed: " + desc);
            observers.forEach(o -> o.onTaskCompleted(t));
        }

        // View all tasks sorted by start time
        public synchronized void viewAllTasks() {
            if (tasksByTime.isEmpty()) {
                System.out.println("No tasks scheduled for the day.");
                return;
            }
            System.out.println("Tasks (sorted by start time):");
            for (Task t : tasksByTime) {
                System.out.println("  " + t.toString());
            }
        }

        public synchronized void viewByPriority(Priority p) {
            boolean found = false;
            for (Task t : tasksByTime) {
                if (t.getPriority() == p) {
                    if (!found) {
                        System.out.println("Tasks with priority " + p + ":");
                        found = true;
                    }
                    System.out.println("  " + t.toString());
                }
            }
            if (!found) System.out.println("No tasks with priority " + p + ".");
        }

        // Validate no overlap with existing tasks (optionally exclude a task)
        private void validateNoOverlap(Task candidate, Task exclude) throws ScheduleException {
            for (Task existing : tasksByTime) {
                if (exclude != null && existing.getId().equals(exclude.getId())) continue;
                if (isOverlap(candidate.getStart(), candidate.getEnd(), existing.getStart(), existing.getEnd())) {
                    // Notify observers about conflict
                    observers.forEach(o -> o.onConflict(candidate, existing));
                    throw new ScheduleException("Task conflicts with existing task \"" + existing.getDescription() + "\".");
                }
            }
        }

        private boolean isOverlap(LocalTime s1, LocalTime e1, LocalTime s2, LocalTime e2) {
            // Overlap if s1 < e2 and s2 < e1
            return s1.isBefore(e2) && s2.isBefore(e1);
        }
    }
}
