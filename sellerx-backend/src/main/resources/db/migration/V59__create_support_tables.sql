-- Support Tickets Table
CREATE TABLE support_tickets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    store_id UUID REFERENCES stores(id),
    ticket_number VARCHAR(20) UNIQUE NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP,
    assigned_to BIGINT REFERENCES users(id)
);

-- Ticket Messages Table
CREATE TABLE ticket_messages (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES support_tickets(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    message TEXT NOT NULL,
    is_admin_reply BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_tickets_user ON support_tickets(user_id);
CREATE INDEX idx_tickets_status ON support_tickets(status);
CREATE INDEX idx_tickets_priority ON support_tickets(priority);
CREATE INDEX idx_tickets_category ON support_tickets(category);
CREATE INDEX idx_tickets_assigned ON support_tickets(assigned_to);
CREATE INDEX idx_tickets_created ON support_tickets(created_at DESC);
CREATE INDEX idx_messages_ticket ON ticket_messages(ticket_id);
CREATE INDEX idx_messages_created ON ticket_messages(created_at);

-- Comments
COMMENT ON TABLE support_tickets IS 'Customer support tickets';
COMMENT ON TABLE ticket_messages IS 'Messages within support tickets';
COMMENT ON COLUMN support_tickets.ticket_number IS 'Unique ticket identifier (e.g., TKT-20240122-001)';
COMMENT ON COLUMN support_tickets.status IS 'OPEN, IN_PROGRESS, WAITING_CUSTOMER, RESOLVED, CLOSED';
COMMENT ON COLUMN support_tickets.priority IS 'LOW, MEDIUM, HIGH, URGENT';
COMMENT ON COLUMN support_tickets.category IS 'TECHNICAL, BILLING, ORDER, PRODUCT, INTEGRATION, OTHER';
