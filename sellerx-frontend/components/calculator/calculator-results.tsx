'use client';

import { Landmark, ListTree, PieChart as PieChartIcon } from 'lucide-react';
import { useState, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';

import { Badge } from '@/components/ui/badge';
import { Card, CardContent } from '@/components/ui/card';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Separator } from '@/components/ui/separator';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import type { CalculatorResult, VatRate } from '@/types/calculator';
import { formatCurrency } from '@/lib/utils/calculator';

import { CalculatorCostChart } from './calculator-cost-chart';
import { CalculatorWaterfall } from './calculator-waterfall';

interface CalculatorResultsProps {
  results: CalculatorResult;
  quantity: number;
  vatRate?: VatRate;
}

const TAB_ORDER = ['cost', 'cost-detail', 'vat'] as const;
type TabValue = (typeof TAB_ORDER)[number];

export function CalculatorResults({ results, quantity, vatRate = 0.20 }: CalculatorResultsProps) {
  const vatRatePercent = (vatRate * 100).toFixed(0);
  const [activeTab, setActiveTab] = useState<TabValue>('cost');
  const directionRef = useRef(1);

  const handleTabChange = (value: string) => {
    const newIndex = TAB_ORDER.indexOf(value as TabValue);
    const oldIndex = TAB_ORDER.indexOf(activeTab);
    directionRef.current = newIndex > oldIndex ? 1 : -1;
    setActiveTab(value as TabValue);
  };

  const slideVariants = {
    enter: (direction: number) => ({
      x: direction > 0 ? 80 : -80,
      opacity: 0,
    }),
    center: {
      x: 0,
      opacity: 1,
    },
    exit: (direction: number) => ({
      x: direction > 0 ? -80 : 80,
      opacity: 0,
    }),
  };

  return (
    <Card>
      <Tabs value={activeTab} onValueChange={handleTabChange} className="w-full">
        <div className="px-4 pt-4">
          <TabsList className="w-full grid grid-cols-3">
            <TabsTrigger value="cost" className="gap-1.5">
              <PieChartIcon className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Maliyet</span>
            </TabsTrigger>
            <TabsTrigger value="cost-detail" className="gap-1.5">
              <ListTree className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">Detaylı Maliyet</span>
            </TabsTrigger>
            <TabsTrigger value="vat" className="gap-1.5">
              <Landmark className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">KDV</span>
            </TabsTrigger>
          </TabsList>
        </div>

        <div className="overflow-hidden">
          <AnimatePresence mode="wait" custom={directionRef.current}>
            <motion.div
              key={activeTab}
              custom={directionRef.current}
              variants={slideVariants}
              initial="enter"
              animate="center"
              exit="exit"
              transition={{ duration: 0.2, ease: 'easeInOut' }}
            >
              {activeTab === 'cost' && (
                <CardContent className="pt-4 pb-4 space-y-4">
                  <div className="grid grid-cols-1 xl:grid-cols-[1fr_auto_1fr] gap-4 xl:gap-6">
                    <div>
                      <p className="text-xs font-medium text-muted-foreground mb-2">Maliyet Dağılımı</p>
                      <CalculatorCostChart costs={results.costs} />
                    </div>
                    <div className="hidden xl:flex items-center self-stretch">
                      <Separator orientation="vertical" className="h-1/2" />
                    </div>
                    <div>
                      <p className="text-xs font-medium text-muted-foreground mb-2">Gelir → Kâr Akışı</p>
                      <CalculatorWaterfall
                        salePriceExVat={results.salePriceExVat}
                        costs={results.costs}
                        netProfit={results.netProfit}
                      />
                    </div>
                  </div>
                </CardContent>
              )}

              {activeTab === 'cost-detail' && (
                <CardContent className="pt-4 pb-4">
                  <div className="flex items-center justify-between mb-3">
                    <p className="text-xs font-medium text-muted-foreground">KDV dahil ve hariç maliyet kalemleri</p>
                    <Badge variant="secondary" className="text-xs">
                      KDV: %{vatRatePercent}
                    </Badge>
                  </div>
                  <div className="overflow-x-auto rounded-md border">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead className="min-w-[120px] text-xs">Kalem</TableHead>
                          <TableHead className="text-right min-w-[60px] text-xs">KDV %</TableHead>
                          <TableHead className="text-right min-w-[90px] text-xs">KDV Dahil</TableHead>
                          <TableHead className="text-right min-w-[90px] text-xs">KDV Hariç</TableHead>
                          {quantity > 1 && (
                            <>
                              <TableHead className="text-right min-w-[100px] text-xs">Top. Dahil</TableHead>
                              <TableHead className="text-right min-w-[100px] text-xs">Top. Hariç</TableHead>
                            </>
                          )}
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {results.costs.map((cost, index) => (
                          <TableRow key={index}>
                            <TableCell className="font-medium text-xs py-2">{cost.name}</TableCell>
                            <TableCell className="text-right text-xs py-2">
                              <Badge variant="outline" className="text-[10px] px-1.5 py-0">
                                %{(cost.rate * 100).toFixed(0)}
                              </Badge>
                            </TableCell>
                            <TableCell className="text-right text-xs py-2">
                              {formatCurrency(cost.amountIncVat)}
                            </TableCell>
                            <TableCell className="text-right text-xs py-2">
                              {formatCurrency(cost.amountExVat)}
                            </TableCell>
                            {quantity > 1 && (
                              <>
                                <TableCell className="text-right text-xs py-2">
                                  {formatCurrency(cost.totalIncVat)}
                                </TableCell>
                                <TableCell className="text-right text-xs py-2">
                                  {formatCurrency(cost.totalExVat)}
                                </TableCell>
                              </>
                            )}
                          </TableRow>
                        ))}
                        <TableRow className="font-semibold bg-muted/50">
                          <TableCell className="text-xs py-2">Toplam</TableCell>
                          <TableCell className="text-right text-xs py-2">-</TableCell>
                          <TableCell className="text-right text-xs font-bold py-2">
                            {formatCurrency(
                              results.costs.reduce((sum, c) => sum + c.amountIncVat, 0)
                            )}
                          </TableCell>
                          <TableCell className="text-right text-xs font-bold py-2">
                            {formatCurrency(
                              results.costs.reduce((sum, c) => sum + c.amountExVat, 0)
                            )}
                          </TableCell>
                          {quantity > 1 && (
                            <>
                              <TableCell className="text-right text-xs font-bold py-2">
                                {formatCurrency(
                                  results.costs.reduce((sum, c) => sum + c.totalIncVat, 0)
                                )}
                              </TableCell>
                              <TableCell className="text-right text-xs font-bold py-2">
                                {formatCurrency(results.totalCostExVat)}
                              </TableCell>
                            </>
                          )}
                        </TableRow>
                      </TableBody>
                    </Table>
                  </div>
                </CardContent>
              )}

              {activeTab === 'vat' && (
                <CardContent className="pt-4 pb-2 overflow-x-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead className="min-w-[120px] text-xs">Kalem</TableHead>
                        <TableHead className="text-right min-w-[80px] text-xs">Tür</TableHead>
                        <TableHead className="text-right min-w-[90px] text-xs">Birim KDV</TableHead>
                        {quantity > 1 && (
                          <TableHead className="text-right min-w-[100px] text-xs">Toplam KDV</TableHead>
                        )}
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      <TableRow>
                        <TableCell className="font-medium text-xs py-2">
                          {results.vatCalculation.grossExpense.name}
                        </TableCell>
                        <TableCell className="text-right py-2">
                          <Badge variant="outline" className="text-[10px] px-1.5 py-0 bg-orange-50 text-orange-600 border-orange-200 dark:bg-orange-950/30 dark:text-orange-400 dark:border-orange-800">
                            Brüt Gider
                          </Badge>
                        </TableCell>
                        <TableCell className="text-right text-xs py-2">
                          {formatCurrency(results.vatCalculation.grossExpense.amount)}
                        </TableCell>
                        {quantity > 1 && (
                          <TableCell className="text-right text-xs py-2">
                            {formatCurrency(results.vatCalculation.grossExpense.total)}
                          </TableCell>
                        )}
                      </TableRow>

                      {results.vatCalculation.incomeItems.map((item, index) => (
                        <TableRow key={index}>
                          <TableCell className="font-medium text-xs py-2">{item.name}</TableCell>
                          <TableCell className="text-right py-2">
                            <Badge variant="outline" className="text-[10px] px-1.5 py-0 bg-green-50 text-green-600 border-green-200 dark:bg-green-950/30 dark:text-green-400 dark:border-green-800">
                              İndirim
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right text-xs py-2 text-green-600 dark:text-green-400">
                            {formatCurrency(item.amount)}
                          </TableCell>
                          {quantity > 1 && (
                            <TableCell className="text-right text-xs py-2 text-green-600 dark:text-green-400">
                              {formatCurrency(item.total)}
                            </TableCell>
                          )}
                        </TableRow>
                      ))}

                      <TableRow className="font-semibold bg-muted/50">
                        <TableCell className="text-xs py-2">Net KDV Gider</TableCell>
                        <TableCell className="text-right py-2">
                          <Badge className="text-[10px] px-1.5 py-0">Net</Badge>
                        </TableCell>
                        <TableCell className="text-right text-xs font-bold py-2">
                          {formatCurrency(results.vatCalculation.netExpense.amount / quantity)}
                        </TableCell>
                        {quantity > 1 && (
                          <TableCell className="text-right text-xs font-bold py-2">
                            {formatCurrency(results.vatCalculation.netExpense.total)}
                          </TableCell>
                        )}
                      </TableRow>
                    </TableBody>
                  </Table>
                </CardContent>
              )}
            </motion.div>
          </AnimatePresence>
        </div>
      </Tabs>
    </Card>
  );
}
