"use client";

import { AnswerFlowSplitView } from "./answer-flow-split-view";

interface AnswerFlowSectionProps {
  storeId: string;
  aiEnabled: boolean;
}

export function AnswerFlowSection({ storeId, aiEnabled }: AnswerFlowSectionProps) {
  return <AnswerFlowSplitView storeId={storeId} aiEnabled={aiEnabled} />;
}
