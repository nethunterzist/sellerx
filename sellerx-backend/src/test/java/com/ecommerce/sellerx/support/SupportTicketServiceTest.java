package com.ecommerce.sellerx.support;

import com.ecommerce.sellerx.common.BaseUnitTest;
import com.ecommerce.sellerx.email.EmailService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.support.dto.*;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SupportTicketServiceTest extends BaseUnitTest {

    @Mock
    private SupportTicketRepository ticketRepository;

    @Mock
    private TicketMessageRepository messageRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    private SupportTicketService supportTicketService;

    private User testUser;
    private User adminUser;
    private Store testStore;
    private SupportTicket testTicket;

    @BeforeEach
    void setUp() {
        supportTicketService = new SupportTicketService(
                ticketRepository, messageRepository, storeRepository, userService, emailService
        );

        testUser = User.builder()
                .name("Test User")
                .email("test@test.com")
                .role(Role.USER)
                .build();
        testUser.setId(1L);

        adminUser = User.builder()
                .name("Admin User")
                .email("admin@test.com")
                .role(Role.ADMIN)
                .build();
        adminUser.setId(2L);

        testStore = Store.builder()
                .storeName("Test Store")
                .user(testUser)
                .build();
        testStore.setId(UUID.randomUUID());

        testTicket = SupportTicket.builder()
                .user(testUser)
                .store(testStore)
                .ticketNumber("TKT-20260131-001")
                .subject("Test Subject")
                .category(TicketCategory.TECHNICAL)
                .priority(TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .messages(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        testTicket.setId(1L);
    }

    @Nested
    @DisplayName("createTicket")
    class CreateTicket {

        @Test
        @DisplayName("should create ticket with store successfully")
        void shouldCreateTicketWithStore() {
            CreateTicketRequest request = CreateTicketRequest.builder()
                    .subject("Login Issue")
                    .message("Cannot login to my account")
                    .category(TicketCategory.TECHNICAL)
                    .priority(TicketPriority.HIGH)
                    .storeId(testStore.getId())
                    .build();

            when(userService.findById(1L)).thenReturn(Optional.of(testUser));
            when(storeRepository.findById(testStore.getId())).thenReturn(Optional.of(testStore));
            when(ticketRepository.getNextTicketSequence()).thenReturn(1);
            when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
                SupportTicket ticket = invocation.getArgument(0);
                ticket.setId(10L);
                return ticket;
            });

            TicketDto result = supportTicketService.createTicket(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getSubject()).isEqualTo("Login Issue");
            assertThat(result.getStatus()).isEqualTo(TicketStatus.OPEN);
            assertThat(result.getPriority()).isEqualTo(TicketPriority.HIGH);
            assertThat(result.getCategory()).isEqualTo(TicketCategory.TECHNICAL);
            assertThat(result.getUserName()).isEqualTo("Test User");
            assertThat(result.getStoreName()).isEqualTo("Test Store");
            assertThat(result.getMessages()).isNotNull().hasSize(1);

            verify(ticketRepository).save(any(SupportTicket.class));
            verify(emailService).sendTicketCreatedEmail(eq("test@test.com"), anyString(), eq("Login Issue"));
        }

        @Test
        @DisplayName("should create ticket without store")
        void shouldCreateTicketWithoutStore() {
            CreateTicketRequest request = CreateTicketRequest.builder()
                    .subject("General Question")
                    .message("How do I use the app?")
                    .category(TicketCategory.OTHER)
                    .priority(TicketPriority.LOW)
                    .build();

            when(userService.findById(1L)).thenReturn(Optional.of(testUser));
            when(ticketRepository.getNextTicketSequence()).thenReturn(1);
            when(ticketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
                SupportTicket ticket = invocation.getArgument(0);
                ticket.setId(10L);
                return ticket;
            });

            TicketDto result = supportTicketService.createTicket(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getStoreId()).isNull();
            assertThat(result.getStoreName()).isNull();
            verify(storeRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should throw when user not found")
        void shouldThrowWhenUserNotFound() {
            CreateTicketRequest request = CreateTicketRequest.builder()
                    .subject("Test")
                    .message("Test message")
                    .category(TicketCategory.TECHNICAL)
                    .build();

            when(userService.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supportTicketService.createTicket(999L, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        @DisplayName("should throw when store not found")
        void shouldThrowWhenStoreNotFound() {
            UUID missingStoreId = UUID.randomUUID();
            CreateTicketRequest request = CreateTicketRequest.builder()
                    .subject("Test")
                    .message("Test message")
                    .category(TicketCategory.TECHNICAL)
                    .storeId(missingStoreId)
                    .build();

            when(userService.findById(1L)).thenReturn(Optional.of(testUser));
            when(storeRepository.findById(missingStoreId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supportTicketService.createTicket(1L, request))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUserTickets")
    class GetUserTickets {

        @Test
        @DisplayName("should return paginated tickets for user")
        void shouldReturnPaginatedTicketsForUser() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SupportTicket> ticketPage = new PageImpl<>(List.of(testTicket), pageable, 1);

            when(ticketRepository.findByUserIdOrderByCreatedAtDesc(1L, pageable)).thenReturn(ticketPage);

            Page<TicketDto> result = supportTicketService.getUserTickets(1L, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTicketNumber()).isEqualTo("TKT-20260131-001");
        }
    }

    @Nested
    @DisplayName("getTicketForUser")
    class GetTicketForUser {

        @Test
        @DisplayName("should return ticket when user owns it")
        void shouldReturnTicketWhenUserOwnsIt() {
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

            TicketDto result = supportTicketService.getTicketForUser(1L, 1L);

            assertThat(result).isNotNull();
            assertThat(result.getTicketNumber()).isEqualTo("TKT-20260131-001");
        }

        @Test
        @DisplayName("should throw when user does not own ticket")
        void shouldThrowWhenUserDoesNotOwnTicket() {
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

            assertThatThrownBy(() -> supportTicketService.getTicketForUser(1L, 999L))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("should throw when ticket not found")
        void shouldThrowWhenTicketNotFound() {
            when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> supportTicketService.getTicketForUser(999L, 1L))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addUserMessage")
    class AddUserMessage {

        @Test
        @DisplayName("should add user message and update status from WAITING_CUSTOMER")
        void shouldAddUserMessageAndUpdateStatus() {
            testTicket.setStatus(TicketStatus.WAITING_CUSTOMER);
            testTicket.setAssignedTo(adminUser);
            AddMessageRequest request = AddMessageRequest.builder().message("Here is my response").build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(userService.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> {
                TicketMessage msg = invocation.getArgument(0);
                msg.setId(100L);
                return msg;
            });

            TicketMessageDto result = supportTicketService.addUserMessage(1L, 1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getMessage()).isEqualTo("Here is my response");
            assertThat(result.getIsAdminReply()).isFalse();
            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
            verify(ticketRepository).save(testTicket);
            verify(emailService).sendTicketReplyEmail(eq("admin@test.com"), anyString(), eq("Test User"), anyString());
        }

        @Test
        @DisplayName("should not update status when not WAITING_CUSTOMER")
        void shouldNotUpdateStatusWhenNotWaitingCustomer() {
            testTicket.setStatus(TicketStatus.IN_PROGRESS);
            AddMessageRequest request = AddMessageRequest.builder().message("More info").build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(userService.findById(1L)).thenReturn(Optional.of(testUser));
            when(messageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> {
                TicketMessage msg = invocation.getArgument(0);
                msg.setId(101L);
                return msg;
            });

            supportTicketService.addUserMessage(1L, 1L, request);

            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when user does not own ticket")
        void shouldThrowWhenUserDoesNotOwnTicket() {
            AddMessageRequest request = AddMessageRequest.builder().message("test").build();
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

            assertThatThrownBy(() -> supportTicketService.addUserMessage(1L, 999L, request))
                    .isInstanceOf(SecurityException.class);
        }
    }

    @Nested
    @DisplayName("addAdminReply")
    class AddAdminReply {

        @Test
        @DisplayName("should add admin reply and set status to WAITING_CUSTOMER")
        void shouldAddAdminReplyAndSetStatus() {
            testTicket.setStatus(TicketStatus.IN_PROGRESS);
            AddMessageRequest request = AddMessageRequest.builder().message("We are looking into this").build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(userService.findById(2L)).thenReturn(Optional.of(adminUser));
            when(messageRepository.save(any(TicketMessage.class))).thenAnswer(invocation -> {
                TicketMessage msg = invocation.getArgument(0);
                msg.setId(200L);
                return msg;
            });

            TicketMessageDto result = supportTicketService.addAdminReply(1L, 2L, request);

            assertThat(result).isNotNull();
            assertThat(result.getIsAdminReply()).isTrue();
            assertThat(result.getMessage()).isEqualTo("We are looking into this");
            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.WAITING_CUSTOMER);
            verify(ticketRepository).save(testTicket);
            verify(emailService).sendTicketReplyEmail(eq("test@test.com"), anyString(), eq("Admin User"), anyString());
        }
    }

    @Nested
    @DisplayName("updateTicketStatus")
    class UpdateTicketStatus {

        @Test
        @DisplayName("should update ticket status")
        void shouldUpdateTicketStatus() {
            UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                    .status(TicketStatus.RESOLVED)
                    .build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(ticketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);

            TicketDto result = supportTicketService.updateTicketStatus(1L, request);

            assertThat(result).isNotNull();
            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
            assertThat(testTicket.getClosedAt()).isNull();
            verify(emailService, never()).sendTicketClosedEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("should close ticket and set closedAt when status is CLOSED")
        void shouldCloseTicketAndSetClosedAt() {
            UpdateTicketStatusRequest request = UpdateTicketStatusRequest.builder()
                    .status(TicketStatus.CLOSED)
                    .build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(ticketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);

            supportTicketService.updateTicketStatus(1L, request);

            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.CLOSED);
            assertThat(testTicket.getClosedAt()).isNotNull();
            verify(emailService).sendTicketClosedEmail("test@test.com", "TKT-20260131-001");
        }
    }

    @Nested
    @DisplayName("assignTicket")
    class AssignTicket {

        @Test
        @DisplayName("should assign ticket to admin and change OPEN to IN_PROGRESS")
        void shouldAssignTicketToAdmin() {
            testTicket.setStatus(TicketStatus.OPEN);
            AssignTicketRequest request = AssignTicketRequest.builder().adminId(2L).build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(userService.findById(2L)).thenReturn(Optional.of(adminUser));
            when(ticketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);

            TicketDto result = supportTicketService.assignTicket(1L, request);

            assertThat(result).isNotNull();
            assertThat(testTicket.getAssignedTo()).isEqualTo(adminUser);
            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
            verify(emailService).sendTicketAssignedEmail(
                    eq("admin@test.com"), eq("TKT-20260131-001"), eq("Test Subject"), eq("Test User"));
        }

        @Test
        @DisplayName("should not change status if already IN_PROGRESS")
        void shouldNotChangeStatusIfAlreadyInProgress() {
            testTicket.setStatus(TicketStatus.IN_PROGRESS);
            AssignTicketRequest request = AssignTicketRequest.builder().adminId(2L).build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(userService.findById(2L)).thenReturn(Optional.of(adminUser));
            when(ticketRepository.save(any(SupportTicket.class))).thenReturn(testTicket);

            supportTicketService.assignTicket(1L, request);

            assertThat(testTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("should throw when assigning to non-admin user")
        void shouldThrowWhenAssigningToNonAdmin() {
            User regularUser = User.builder().name("Regular").email("regular@test.com").role(Role.USER).build();
            regularUser.setId(3L);
            AssignTicketRequest request = AssignTicketRequest.builder().adminId(3L).build();

            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
            when(userService.findById(3L)).thenReturn(Optional.of(regularUser));

            assertThatThrownBy(() -> supportTicketService.assignTicket(1L, request))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("searchTickets")
    class SearchTickets {

        @Test
        @DisplayName("should search tickets by query")
        void shouldSearchTicketsByQuery() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SupportTicket> ticketPage = new PageImpl<>(List.of(testTicket), pageable, 1);

            when(ticketRepository.searchTickets("TKT", pageable)).thenReturn(ticketPage);

            Page<TicketDto> result = supportTicketService.searchTickets("TKT", pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTicketNumber()).isEqualTo("TKT-20260131-001");
        }
    }

    @Nested
    @DisplayName("getTicketStats")
    class GetTicketStats {

        @Test
        @DisplayName("should return aggregated ticket statistics")
        void shouldReturnAggregatedTicketStats() {
            when(ticketRepository.count()).thenReturn(100L);
            when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(20L);
            when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(30L);
            when(ticketRepository.countByStatus(TicketStatus.WAITING_CUSTOMER)).thenReturn(10L);
            when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(15L);
            when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(25L);

            TicketStatsDto result = supportTicketService.getTicketStats();

            assertThat(result.getTotalTickets()).isEqualTo(100L);
            assertThat(result.getOpenTickets()).isEqualTo(20L);
            assertThat(result.getInProgressTickets()).isEqualTo(30L);
            assertThat(result.getWaitingCustomerTickets()).isEqualTo(10L);
            assertThat(result.getResolvedTickets()).isEqualTo(15L);
            assertThat(result.getClosedTickets()).isEqualTo(25L);
        }
    }

    @Nested
    @DisplayName("getAllTickets (admin)")
    class GetAllTickets {

        @Test
        @DisplayName("should return filtered tickets for admin")
        void shouldReturnFilteredTicketsForAdmin() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SupportTicket> ticketPage = new PageImpl<>(List.of(testTicket), pageable, 1);

            when(ticketRepository.findWithFilters(TicketStatus.OPEN, null, null, pageable))
                    .thenReturn(ticketPage);

            Page<TicketDto> result = supportTicketService.getAllTickets(
                    TicketStatus.OPEN, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getTicketForAdmin")
    class GetTicketForAdmin {

        @Test
        @DisplayName("should return ticket for admin without ownership check")
        void shouldReturnTicketForAdmin() {
            when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

            TicketDto result = supportTicketService.getTicketForAdmin(1L);

            assertThat(result).isNotNull();
            assertThat(result.getTicketNumber()).isEqualTo("TKT-20260131-001");
            assertThat(result.getMessages()).isNotNull();
        }
    }
}
