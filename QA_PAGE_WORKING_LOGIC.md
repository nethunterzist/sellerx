# QA Page Working Logic - Simple Documentation

**Page URL**: `http://localhost:3000/tr/qa`
**Main Component**: `sellerx-frontend/app/[locale]/(app-shell)/qa/page.tsx`

---

## 🎯 Purpose
AI-powered Question & Answer system for e-commerce customer service. System learns from answered questions and suggests automated responses.

---

## 📊 Page Structure

### 4 Main Tabs (Card-Based Navigation)

#### 1️⃣ **Answer Flow** (default open)
- **Component**: `answer-flow-section.tsx`
- **Purpose**: Question queue management
- **Sections**:
  - **Pending Approval** (yellow) - AI suggested answers waiting for approval
  - **Auto Answered** (green) - Questions answered automatically by AI (collapsed by default)
  - **Human Required** (orange) - Low confidence questions needing manual answers (empty for now)
- **Each question shows**: Customer question, AI suggested answer, confidence score, approve/reject buttons

#### 2️⃣ **AI Brain** (Knowledge Hub)
- **Component**: `knowledge-hub.tsx`
- **Purpose**: Manage AI knowledge base
- **Two sections**:
  - **Knowledge Base**: User-created answer templates (title, content, keywords, category)
  - **Learning Suggestions**: AI-discovered patterns from repeated questions (approve/edit/reject)
- **Actions**: Add, edit, delete, activate/deactivate knowledge items

#### 3️⃣ **Rules** (Settings)
- **Component**: `rules-section.tsx`
- **Purpose**: Configure AI answer behavior
- **Settings**:
  - Tone (professional/friendly/formal)
  - Language (TR/EN)
  - Max answer length
  - Include greeting/signature
  - **Confidence threshold** (slider 0-100%) - minimum confidence to show AI answer
  - **Auto answer** (ON/OFF) - automatically send answers above threshold
- **Safety Rules**: Hardcoded rules for legal, health, brand conflicts (display only)

#### 4️⃣ **Performance** (Analytics)
- **Component**: `performance-section.tsx`
- **Purpose**: AI performance tracking
- **Stats Cards**: Total patterns, auto-submit eligible, active alerts, avg approval rate
- **Seniority System**:
  - JUNIOR (○) → LEARNING (◐) → SENIOR (●) → EXPERT (★)
  - Tracks AI confidence evolution for each question pattern
  - Users can promote/demote patterns manually
- **Conflict Alerts**: Shows contradicting answers or safety rule violations

---

## 🔄 Data Flow

```
Frontend (React Query hooks)
  ↓
Next.js API Routes (/api/qa/*) [BFF Pattern]
  ↓
Spring Boot Backend (/api/qa/stores/{storeId}/*)
  ↓
PostgreSQL Database
```

**Key Hooks** (in `hooks/queries/use-qa.ts`):
- `useQuestions()` - Fetch questions by status (PENDING/ANSWERED)
- `useQaStats()` - Overall statistics
- `useSuggestions()` - AI-discovered knowledge suggestions
- `usePatterns()` - Seniority-based answer patterns
- `useConflicts()` - Conflict alerts
- `useSubmitAnswer()` - Manual answer submission

---

## 🎨 Key UI Features

1. **Tab Navigation**: 4 colored cards with icons and badges
2. **Motion**: `motion/react` for smooth tab transitions (fade effect)
3. **Collapsible Sections**: Answer Flow uses `AnimateHeight` for expand/collapse
4. **Real-time Updates**: React Query auto-refetch after mutations
5. **Pagination**: Answer Flow sections have load more functionality
6. **Dark Mode Support**: All components use Tailwind dark mode classes

---

## 🧠 AI Learning Flow

1. **Customer asks question** → Backend creates `Question` entity (status: PENDING)
2. **AI analyzes** → Searches knowledge base, calculates confidence score
3. **If confidence > threshold** → Suggests answer (shown in "Pending Approval")
4. **User approves** → Answer sent to customer, pattern created/updated
5. **Pattern matures** → JUNIOR → LEARNING → SENIOR → EXPERT (based on approval rate)
6. **EXPERT pattern** → Eligible for auto-submit (skips approval step)
7. **Repeated similar questions** → AI creates "Learning Suggestion" for knowledge base

---

## 🎯 Design Change Considerations

**Current Layout**:
- Top: 4 tab cards (2 cols mobile, 4 cols desktop)
- Below: Tab content with fade animation

**Component Hierarchy**:
```
qa/page.tsx (main container)
├── Tab Cards (4 cards with onClick handlers)
└── AnimatePresence
    ├── AnswerFlowSection
    │   ├── CollapsibleSection (Pending Approval)
    │   ├── CollapsibleSection (Auto Answered)
    │   └── CollapsibleSection (Human Required)
    ├── KnowledgeHub
    │   ├── Knowledge Base Card
    │   └── Learning Suggestions Card
    ├── RulesSection
    │   ├── Tone & Style Card
    │   ├── Safety Rules Card
    │   └── Auto Answer Policy Card
    └── PerformanceSection
        ├── Summary Stats (4 cards)
        ├── Seniority Status Card
        └── Alerts Card
```

**Colors by Section**:
- Answer Flow: Yellow (pending), Green (answered), Orange (human)
- AI Brain: Blue (knowledge), Yellow (suggestions)
- Rules: Purple (tone), Red (safety), Blue (policy)
- Performance: Purple (seniority), Yellow (alerts)

**Shared Components Used**:
- `Card`, `CardHeader`, `CardTitle`, `CardContent` - Layout containers
- `Button`, `Badge`, `Switch`, `Input`, `Textarea` - Form elements
- `Select`, `SelectTrigger`, `SelectContent`, `SelectItem` - Dropdowns
- `Slider` - Confidence threshold adjustment
- `StaggerChildren` - Staggered animation wrapper for cards
- `AnimateHeight` - Smooth expand/collapse
- `NumberTicker` - Animated number counters

---

## 📦 Dependencies
- **UI**: `shadcn/ui` components (Radix primitives + Tailwind)
- **Animation**: `motion/react` (Framer Motion)
- **State**: React Query for server state
- **i18n**: `next-intl` for translations
- **Icons**: `lucide-react`

---

## ⚙️ Backend Endpoints Used

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/qa/stores/{storeId}/questions` | GET | List questions with pagination |
| `/api/qa/stores/{storeId}/questions/{questionId}/answer` | POST | Submit manual answer |
| `/api/qa/stores/{storeId}/stats` | GET | Overall Q&A statistics |
| `/api/qa/stores/{storeId}/suggestions` | GET | AI learning suggestions |
| `/api/qa/suggestions/{id}/approve` | POST | Approve knowledge suggestion |
| `/api/qa/suggestions/{id}/reject` | POST | Reject knowledge suggestion |
| `/api/qa/stores/{storeId}/patterns` | GET | Seniority patterns |
| `/api/qa/patterns/{id}/promote` | POST | Promote pattern seniority |
| `/api/qa/patterns/{id}/demote` | POST | Demote pattern seniority |
| `/api/qa/stores/{storeId}/conflicts` | GET | Conflict alerts |

---

**End of Document** - Ready for Gemini analysis 🤖
