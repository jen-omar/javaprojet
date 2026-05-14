package tn.esprit.services;

import org.junit.jupiter.api.*;
import tn.esprit.Models.Proposal;
import tn.esprit.util.MyConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Workshop: Unit Testing CRUD Operations
 * This class demonstrates how to test a Service layer using JUnit 5.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Ensures tests run in a specific order
public class ProposalServiceTest {

    private static ProposalService proposalService;
    private static int testProposalId; // To track the ID of the created entity
    
    // We use static IDs for artist and brief that we know exist in the DB for this workshop
    // In a real scenario, you would create these entities first.
    private static final int TEST_ARTIST_ID = 1; 
    private static final int TEST_BRIEF_ID = 1;

    @BeforeAll
    static void setup() {
        // Initializing the service before all tests
        proposalService = new ProposalService();
        System.out.println("🚀 Starting Proposal Service Workshop Tests...");
    }

    @BeforeEach
    void init() {
        // Runs before each @Test method
        System.out.println("--- Executing Test Case ---");
    }

    @Test
    @Order(1) // Step 1: Create
    @DisplayName("1. Create: Should add a new proposal to the database")
    void testAddProposal() {
        Proposal p = new Proposal(
                500.0, 
                5, 
                "Test Cover Letter", 
                LocalDateTime.now(), 
                false, 
                TEST_ARTIST_ID, 
                TEST_BRIEF_ID
        );

        // Action
        proposalService.add(p);

        // Verification: Retrieve and check
        List<Proposal> all = proposalService.getAll();
        Proposal lastAdded = all.stream()
                .filter(prop -> prop.getCoverLetter().equals("Test Cover Letter"))
                .findFirst()
                .orElse(null);

        assertNotNull(lastAdded, "The proposal should be saved in the database");
        assertEquals(500.0, lastAdded.getPrice(), "Price mismatch");
        testProposalId = lastAdded.getId(); // Save ID for next steps
        
        System.out.println("✅ Create Successful: ID = " + testProposalId);
    }

    @Test
    @Order(2) // Step 2: Read
    @DisplayName("2. Read: Should retrieve the proposal by its properties")
    void testGetProposals() {
        List<Proposal> proposals = proposalService.getAll();
        
        // Assertions
        assertFalse(proposals.isEmpty(), "List should not be empty");
        assertTrue(proposals.stream().anyMatch(p -> p.getId() == testProposalId), 
                "Should contain the proposal created in Step 1");
        
        System.out.println("✅ Read Successful: Proposal found in list");
    }

    @Test
    @Order(3) // Step 3: Update
    @DisplayName("3. Update: Should modify the price of an existing proposal")
    void testUpdateProposal() {
        // Prepare updated data
        Proposal p = new Proposal(
                testProposalId,
                750.0, // New Price
                7, 
                "Updated Cover Letter", 
                LocalDateTime.now(), 
                true, 
                TEST_ARTIST_ID, 
                TEST_BRIEF_ID
        );

        // Action
        proposalService.update(p);

        // Verification
        List<Proposal> all = proposalService.getAll();
        Proposal updated = all.stream()
                .filter(prop -> prop.getId() == testProposalId)
                .findFirst()
                .orElse(null);

        assertNotNull(updated);
        assertEquals(750.0, updated.getPrice(), "Update failed: Price not changed");
        assertTrue(updated.isAccepted(), "Update failed: is_accepted not changed");
        
        System.out.println("✅ Update Successful: Price changed to 750.0");
    }

    @Test
    @Order(4) // Step 4: Delete
    @DisplayName("4. Delete: Should remove the proposal from the database")
    void testDeleteProposal() {
        // Action
        proposalService.delete(testProposalId);

        // Verification
        List<Proposal> all = proposalService.getAll();
        boolean exists = all.stream().anyMatch(p -> p.getId() == testProposalId);

        assertFalse(exists, "Delete failed: Proposal still exists in DB");
        
        System.out.println("✅ Delete Successful: Proposal removed");
    }

    @AfterEach
    void tearDown() {
        // Runs after each @Test method
        System.out.println("--- Test Case Completed ---");
    }

    @AfterAll
    static void cleanup() {
        // Cleanup mechanism to ensure database stays clean
        System.out.println("🧹 Cleaning up remaining test data...");
        String cleanupQuery = "DELETE FROM proposal WHERE cover_letter LIKE '%Test%'";
        try (Connection cnx = MyConnection.getInstance().getConnection();
             Statement st = cnx.createStatement()) {
            st.executeUpdate(cleanupQuery);
            System.out.println("✨ Database is clean.");
        } catch (SQLException e) {
            System.err.println("❌ Cleanup error: " + e.getMessage());
        }
    }
}
