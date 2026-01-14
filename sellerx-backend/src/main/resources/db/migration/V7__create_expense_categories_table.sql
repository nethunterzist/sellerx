-- Create expense_categories table
CREATE TABLE IF NOT EXISTS expense_categories (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default categories
INSERT INTO expense_categories (name) VALUES 
('Ambalaj'),
('Kargo'),
('Reklam'),
('Ofis'),
('Muhasebe'),
('Diğer');
