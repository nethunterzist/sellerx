-- Create store_expenses table
CREATE TABLE IF NOT EXISTS store_expenses (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    expense_category_id UUID NOT NULL,
    store_id UUID NOT NULL,
    product_id UUID,  -- NULL means general expense
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    frequency VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (expense_category_id) REFERENCES expense_categories(id) ON DELETE RESTRICT,
    FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES trendyol_products(id) ON DELETE CASCADE
);

-- Create indexes for better performance
CREATE INDEX idx_store_expenses_store_id ON store_expenses(store_id);
CREATE INDEX idx_store_expenses_category_id ON store_expenses(expense_category_id);
CREATE INDEX idx_store_expenses_product_id ON store_expenses(product_id);
CREATE INDEX idx_store_expenses_date ON store_expenses(date);

-- Add check constraint for frequency
ALTER TABLE store_expenses ADD CONSTRAINT check_frequency 
    CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY', 'ONE_TIME'));
