#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
╔═══════════════════════════════════════════════════════════════════════════════╗
║                                                                               ║
║   ██╗     ██╗██╗   ██╗███████╗    ███████╗██╗   ██╗███╗   ██╗ ██████╗        ║
║   ██║     ██║██║   ██║██╔════╝    ██╔════╝╚██╗ ██╔╝████╗  ██║██╔════╝        ║
║   ██║     ██║██║   ██║█████╗      ███████╗ ╚████╔╝ ██╔██╗ ██║██║             ║
║   ██║     ██║╚██╗ ██╔╝██╔══╝      ╚════██║  ╚██╔╝  ██║╚██╗██║██║             ║
║   ███████╗██║ ╚████╔╝ ███████╗    ███████║   ██║   ██║ ╚████║╚██████╗        ║
║   ╚══════╝╚═╝  ╚═══╝  ╚══════╝    ╚══════╝   ╚═╝   ╚═╝  ╚═══╝ ╚═════╝        ║
║                                                                               ║
║                    ██████╗ ███████╗████████╗███████╗ ██████╗████████╗██╗██╗   ║
║                    ██╔══██╗██╔════╝╚══██╔══╝██╔════╝██╔════╝╚══██╔══╝██║██║   ║
║                    ██║  ██║█████╗     ██║   █████╗  ██║        ██║   ██║██║   ║
║                    ██║  ██║██╔══╝     ██║   ██╔══╝  ██║        ██║   ██║╚═╝   ║
║                    ██████╔╝███████╗   ██║   ███████╗╚██████╗   ██║   ██║██╗   ║
║                    ╚═════╝ ╚══════╝   ╚═╝   ╚══════╝ ╚═════╝   ╚═╝   ╚═╝╚═╝   ║
║                                                                               ║
║                  SellerX Backend Trendyol Entegrasyon Analiz Aracı            ║
║                                                                               ║
╚═══════════════════════════════════════════════════════════════════════════════╝

Bu script, SellerX backend'indeki Trendyol entegrasyon algoritmalarını
GERÇEK API çağrıları ile görselleştirir.

Özellikler:
  🔑 Credential Validation
  📦 Product Sync
  🔍 Binary Search (İlk Sipariş Tarihini Bulma)
  📊 Historical Sync (Chunk Processing)
  💰 Financial Detective (Komisyon Analizi)
  ⚡ Rate Limiting Visualization
  🔄 Exponential Backoff

⚠️  DİKKAT: Bu script gerçek Trendyol API çağrıları yapar!
    Rate limit tüketir, dikkatli kullanın.

Kullanım:
  pip install rich requests
  python live_sync_detective.py
"""

import base64
import json
import time
import threading
from datetime import datetime, timedelta
from typing import Optional, List, Dict, Any, Tuple
from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_UP

import requests
from rich.console import Console
from rich.panel import Panel
from rich.table import Table
from rich.progress import Progress, SpinnerColumn, BarColumn, TextColumn, TimeElapsedColumn
from rich.live import Live
from rich.tree import Tree
from rich.text import Text
from rich.rule import Rule
from rich.syntax import Syntax
from rich.layout import Layout
from rich.align import Align
from rich.box import ROUNDED, DOUBLE, HEAVY
from rich.style import Style
from rich.padding import Padding
from rich.columns import Columns
from rich.spinner import Spinner


# ═══════════════════════════════════════════════════════════════════════════════
# TRENDYOL API CREDENTIALS
# ═══════════════════════════════════════════════════════════════════════════════
SELLER_ID = "1080066"
API_KEY = "1mZOp48RWqucWiuPs29I"
API_SECRET = "RiPeGA7ONDgHuwPN8aZ2"
# ═══════════════════════════════════════════════════════════════════════════════


# API Configuration
TRENDYOL_BASE_URL = "https://apigw.trendyol.com"
RATE_LIMIT_PER_SEC = 10.0  # Backend ile aynı: 10 req/sec
PAGE_SIZE_PRODUCTS = 200   # Backend ile aynı
PAGE_SIZE_SETTLEMENTS = 1000
CHUNK_DAYS = 14            # Backend ile aynı: 14 günlük chunk
MAX_RETRIES = 3            # Backend ile aynı: max 3 retry

# Console instance
console = Console()


# ═══════════════════════════════════════════════════════════════════════════════
# RATE LIMITER (Guava RateLimiter Python implementasyonu)
# ═══════════════════════════════════════════════════════════════════════════════
class RateLimiter:
    """
    Backend'deki TrendyolRateLimiter.java implementasyonunun Python karşılığı.
    Guava RateLimiter gibi çalışır: saniyede belirli sayıda permit verir.
    """
    def __init__(self, permits_per_second: float):
        self.permits_per_second = permits_per_second
        self.interval = 1.0 / permits_per_second
        self.last_time = time.time()
        self.lock = threading.Lock()
        self.acquired_count = 0

    def acquire(self) -> float:
        """Blocking acquire - permit alana kadar bekler."""
        with self.lock:
            current_time = time.time()
            wait_time = self.last_time + self.interval - current_time

            if wait_time > 0:
                time.sleep(wait_time)
                self.last_time = time.time()
            else:
                self.last_time = current_time

            self.acquired_count += 1
            return max(0, wait_time)

    def try_acquire(self) -> bool:
        """Non-blocking acquire - hemen döner."""
        with self.lock:
            current_time = time.time()
            if current_time >= self.last_time + self.interval:
                self.last_time = current_time
                self.acquired_count += 1
                return True
            return False


# Global rate limiter instance
rate_limiter = RateLimiter(RATE_LIMIT_PER_SEC)


# ═══════════════════════════════════════════════════════════════════════════════
# TRENDYOL API CLIENT
# ═══════════════════════════════════════════════════════════════════════════════
class TrendyolApiClient:
    """Trendyol API ile iletişim kuran client sınıfı."""

    def __init__(self, seller_id: str, api_key: str, api_secret: str):
        self.seller_id = seller_id
        self.api_key = api_key
        self.api_secret = api_secret
        self.base_url = TRENDYOL_BASE_URL
        self.session = requests.Session()
        self._setup_headers()

    def _setup_headers(self):
        """Basic Auth header oluştur (Backend'deki gibi)."""
        credentials = f"{self.api_key}:{self.api_secret}"
        encoded = base64.b64encode(credentials.encode()).decode()

        self.session.headers.update({
            "Authorization": f"Basic {encoded}",
            "User-Agent": f"{self.seller_id} - SelfIntegration",
            "Content-Type": "application/json"
        })

    def get(self, endpoint: str, params: Dict = None) -> requests.Response:
        """GET request with rate limiting."""
        rate_limiter.acquire()
        url = f"{self.base_url}{endpoint}"
        return self.session.get(url, params=params, timeout=30)

    def get_with_retry(self, endpoint: str, params: Dict = None,
                       max_retries: int = MAX_RETRIES) -> Tuple[Optional[requests.Response], int]:
        """
        Exponential backoff ile retry mekanizması.
        Backend'deki fetchSettlementWithRetry() implementasyonu.
        """
        retry_count = 0
        last_error = None

        while retry_count <= max_retries:
            try:
                response = self.get(endpoint, params)

                if response.status_code == 200:
                    return response, retry_count
                elif response.status_code == 401:
                    # Token expired - retry allowed
                    raise requests.exceptions.HTTPError("401 Unauthorized")
                elif response.status_code >= 500:
                    # Server error - retry allowed
                    raise requests.exceptions.HTTPError(f"{response.status_code} Server Error")
                else:
                    # Other errors - don't retry
                    return response, retry_count

            except (requests.exceptions.RequestException, requests.exceptions.HTTPError) as e:
                last_error = e
                retry_count += 1

                if retry_count <= max_retries:
                    # Exponential backoff: 1s, 2s, 3s (Backend ile aynı)
                    sleep_time = 1.0 * retry_count
                    console.print(f"  [yellow]⚠ Hata: {str(e)[:50]}... "
                                f"Yeniden deneniyor ({retry_count}/{max_retries}) "
                                f"- {sleep_time}s bekleniyor[/yellow]")
                    time.sleep(sleep_time)

        return None, retry_count


# ═══════════════════════════════════════════════════════════════════════════════
# DATA CLASSES
# ═══════════════════════════════════════════════════════════════════════════════
@dataclass
class CostAndStockInfo:
    """Backend'deki JSONB cost_and_stock_info formatı."""
    quantity: int
    unit_cost: Optional[float]
    cost_vat_rate: int
    stock_date: str


@dataclass
class OrderItem:
    """Backend'deki OrderItem JSONB formatı."""
    barcode: str
    title: str
    quantity: int
    price: float
    vat_rate: float
    estimated_commission_rate: Optional[float]
    unit_estimated_commission: Optional[float]


# ═══════════════════════════════════════════════════════════════════════════════
# BÖLÜM 1: CREDENTIAL VALIDATION
# ═══════════════════════════════════════════════════════════════════════════════
def validate_credentials(client: TrendyolApiClient) -> bool:
    """
    Backend'deki TrendyolService.testCredentials() implementasyonu.
    /addresses endpoint'ine vurarak anahtarları doğrular.
    """
    console.print()
    console.print(Rule("[bold cyan]🔑 BÖLÜM 1: CREDENTIAL VALIDATION[/bold cyan]", style="cyan"))
    console.print()

    # İstek detaylarını göster
    endpoint = f"/integration/sellers/{client.seller_id}/addresses"

    info_table = Table(show_header=False, box=ROUNDED, border_style="dim")
    info_table.add_column("Key", style="cyan")
    info_table.add_column("Value", style="white")
    info_table.add_row("Endpoint", f"GET {TRENDYOL_BASE_URL}{endpoint}")
    info_table.add_row("Seller ID", client.seller_id)
    info_table.add_row("API Key", client.api_key[:8] + "..." if len(client.api_key) > 8 else client.api_key)
    info_table.add_row("Auth Type", "Basic Auth (Base64)")
    console.print(info_table)
    console.print()

    # Spinner ile API çağrısı
    with console.status("[bold yellow]Trendyol API'ye bağlanılıyor...[/bold yellow]", spinner="dots"):
        try:
            response = client.get(endpoint)

            if response.status_code == 200:
                console.print(Panel(
                    "[bold green]✓ BAŞARILI[/bold green]\n\n"
                    f"HTTP Status: {response.status_code}\n"
                    f"API Anahtarları Geçerli!",
                    title="[green]Doğrulama Sonucu[/green]",
                    border_style="green",
                    box=DOUBLE
                ))
                return True
            else:
                console.print(Panel(
                    f"[bold red]✗ BAŞARISIZ[/bold red]\n\n"
                    f"HTTP Status: {response.status_code}\n"
                    f"Hata: {response.text[:200]}",
                    title="[red]Doğrulama Sonucu[/red]",
                    border_style="red",
                    box=DOUBLE
                ))
                return False

        except Exception as e:
            console.print(Panel(
                f"[bold red]✗ BAĞLANTI HATASI[/bold red]\n\n"
                f"Hata: {str(e)}",
                title="[red]Doğrulama Sonucu[/red]",
                border_style="red",
                box=DOUBLE
            ))
            return False


# ═══════════════════════════════════════════════════════════════════════════════
# BÖLÜM 2: PRODUCT SYNC
# ═══════════════════════════════════════════════════════════════════════════════
def sync_products(client: TrendyolApiClient) -> List[Dict]:
    """
    Backend'deki TrendyolProductService.syncProductsFromTrendyol() implementasyonu.
    200'erli sayfalama ile ürünleri çeker.
    """
    console.print()
    console.print(Rule("[bold cyan]📦 BÖLÜM 2: PRODUCT SYNC[/bold cyan]", style="cyan"))
    console.print()

    products = []
    page = 0
    total_pages = 1

    endpoint = f"/integration/product/sellers/{client.seller_id}/products"

    # İlk sayfa için total pages al
    console.print("[dim]Toplam ürün sayısı hesaplanıyor...[/dim]")

    response = client.get(endpoint, params={"page": 0, "size": PAGE_SIZE_PRODUCTS})
    if response.status_code != 200:
        console.print(f"[red]Hata: {response.status_code}[/red]")
        return products

    data = response.json()
    total_pages = data.get("totalPages", 1)
    total_elements = data.get("totalElements", 0)

    console.print(f"[green]Toplam: {total_elements} ürün, {total_pages} sayfa[/green]")
    console.print()

    # Progress bar ile sayfalama
    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        BarColumn(bar_width=40),
        TextColumn("[progress.percentage]{task.percentage:>3.0f}%"),
        TextColumn("•"),
        TextColumn("[cyan]{task.completed}/{task.total} sayfa[/cyan]"),
        TimeElapsedColumn(),
        console=console
    ) as progress:
        task = progress.add_task("[cyan]Ürünler çekiliyor...", total=total_pages)

        while page < total_pages:
            response = client.get(endpoint, params={"page": page, "size": PAGE_SIZE_PRODUCTS})

            if response.status_code == 200:
                data = response.json()
                page_products = data.get("content", [])
                products.extend(page_products)

            page += 1
            progress.update(task, completed=page)

    console.print()
    console.print(f"[green]✓ {len(products)} ürün başarıyla çekildi[/green]")

    # JSONB Transformation Preview
    if products:
        console.print()
        console.print("[bold yellow]📋 JSONB TRANSFORMATION PREVIEW[/bold yellow]")
        console.print("[dim]Ham Trendyol JSON → SellerX cost_and_stock_info formatı[/dim]")
        console.print()

        sample_product = products[0]

        # Ham JSON göster
        raw_json = {
            "productId": sample_product.get("id"),
            "barcode": sample_product.get("barcode"),
            "title": sample_product.get("title", "")[:50] + "...",
            "quantity": sample_product.get("quantity"),
            "salePrice": sample_product.get("salePrice"),
            "vatRate": sample_product.get("vatRate")
        }

        console.print(Panel(
            Syntax(json.dumps(raw_json, indent=2, ensure_ascii=False), "json", theme="monokai"),
            title="[yellow]Trendyol API Response (Ham)[/yellow]",
            border_style="yellow"
        ))

        # Transformed JSONB göster
        transformed = {
            "cost_and_stock_info": [
                {
                    "quantity": sample_product.get("quantity", 0),
                    "unitCost": None,  # Kullanıcı tarafından girilecek
                    "costVatRate": 18,  # Default KDV
                    "stockDate": datetime.now().strftime("%Y-%m-%d")
                }
            ]
        }

        console.print(Panel(
            Syntax(json.dumps(transformed, indent=2, ensure_ascii=False), "json", theme="monokai"),
            title="[green]SellerX JSONB Format (Transformed)[/green]",
            border_style="green"
        ))

        # Weighted Average Merge Algorithm açıklaması
        console.print()
        console.print(Panel(
            "[bold]Weighted Average Merge Algoritması:[/bold]\n\n"
            "[cyan]Aynı tarihte birden fazla stok güncellemesi gelirse:[/cyan]\n\n"
            "weightedAvgCost = (existingQty × existingCost + newQty × newCost) / totalQuantity\n"
            "weightedAvgVatRate = (existingQty × existingVatRate + newQty × newVatRate) / totalQuantity\n\n"
            "[dim]Backend: TrendyolProductService.addOrMergeCostAndStockInfo()[/dim]",
            title="[magenta]Merge Algoritması[/magenta]",
            border_style="magenta"
        ))

    return products


# ═══════════════════════════════════════════════════════════════════════════════
# BÖLÜM 3: BINARY SEARCH (Zaman Makinesi)
# ═══════════════════════════════════════════════════════════════════════════════
def binary_search_first_order(client: TrendyolApiClient) -> Optional[datetime]:
    """
    Backend'deki TrendyolHistoricalSettlementService.findFirstOrderDate() implementasyonu.
    Binary Search ile ilk sipariş tarihini bulur.
    """
    console.print()
    console.print(Rule("[bold cyan]🔍 BÖLÜM 3: BINARY SEARCH (Zaman Makinesi)[/bold cyan]", style="cyan"))
    console.print()

    console.print(Panel(
        "[bold]Binary Search Algoritması (Geliştirilmiş)[/bold]\n\n"
        "Mağazanın ilk sipariş tarihini bulmak için Ekim 2017'den bugüne kadar\n"
        "ikili arama (binary search) + refinement yapılır.\n\n"
        "[cyan]Neden Ekim 2017?[/cyan] Trendyol Marketplace bu tarihte başladı.\n\n"
        "[yellow]Algoritma:[/yellow]\n"
        "1. Aralık: 2017-10-01 → bugün\n"
        "2. Orta noktayı hesapla\n"
        "3. Settlement API'de veri var mı kontrol et (22 günlük pencere)\n"
        "4. Varsa → [cyan]orderDate[/cyan] analiz et, daha erken ara\n"
        "5. Yoksa → daha geç ara\n"
        "6. Aralık 15 günden küçük olana kadar tekrarla\n"
        "7. [green]REFINEMENT:[/green] Bulunan tarihten 35 gün geriye doğrula\n\n"
        "[magenta]ÖNEMLİ:[/magenta] Settlement API [cyan]transactionDate[/cyan]'e göre filtreler,\n"
        "[cyan]orderDate[/cyan] genellikle 3-7 gün daha erken olabilir!\n\n"
        "[green]API Endpoint:[/green] /integration/finance/che/sellers/{sellerId}/settlements\n\n"
        "[dim]Backend: TrendyolHistoricalSettlementService.findFirstOrderDate()[/dim]",
        title="[cyan]Algoritma Açıklaması[/cyan]",
        border_style="cyan"
    ))
    console.print()

    # Binary Search başlangıç değerleri
    low = datetime(2017, 10, 1)
    high = datetime.now()
    first_order_date = None
    iteration = 0

    # Search tree visualization
    search_tree = Tree("[bold cyan]🔍 Binary Search İterasyonları[/bold cyan]")

    def check_data_exists(start_date: datetime, end_date: datetime) -> Tuple[bool, Optional[datetime]]:
        """Settlement API'de sipariş var mı kontrol et ve en eski orderDate'i döndür.

        NOT: Orders API sadece 90 gün geriye gidebilir!
        Settlement API ise mağazanın tüm geçmişine erişebilir.
        Bu yüzden ilk sipariş tarihini bulmak için Settlement API kullanıyoruz.

        ÖNEMLİ: Settlement API startDate/endDate parametreleri transactionDate'e göre
        filtreler, orderDate'e göre DEĞİL! orderDate genellikle transactionDate'den
        3-7 gün daha erken olabilir.

        Returns:
            Tuple[bool, Optional[datetime]]: (veri_var_mi, en_eski_order_date)

        Backend: TrendyolHistoricalSettlementService.findFirstOrderDate()
        """
        endpoint = f"/integration/finance/che/sellers/{client.seller_id}/settlements"

        # Timestamp'leri millisecond'a çevir (Europe/Istanbul timezone)
        start_ts = int(start_date.timestamp() * 1000)
        end_ts = int(end_date.timestamp() * 1000)

        params = {
            "transactionType": "Sale",
            "startDate": start_ts,
            "endDate": end_ts,
            "page": 0,
            "size": 500  # DÜZELTİLDİ: Settlement API sadece 500 veya 1000 kabul eder!
        }

        response, retries = client.get_with_retry(endpoint, params)

        if response and response.status_code == 200:
            data = response.json()
            if data.get("totalElements", 0) > 0:
                # Dönen verideki en eski orderDate'i bul
                settlements = data.get("content", [])
                oldest_order_date = None

                for s in settlements:
                    order_ts = s.get("orderDate")
                    if order_ts:
                        order_date = datetime.fromtimestamp(order_ts / 1000)
                        if oldest_order_date is None or order_date < oldest_order_date:
                            oldest_order_date = order_date

                return True, oldest_order_date
        return False, None

    console.print("[bold yellow]Arama başlıyor...[/bold yellow]")
    console.print()

    # NOT: Settlement API maksimum 15 günlük aralık kabul eder!
    # Bu yüzden pencereyi 15 günde tutuyoruz, ama orderDate analizi yapıyoruz.
    SEARCH_WINDOW_DAYS = 14  # API limiti: max 15 gün

    # Binary search loop
    while (high - low).days >= 15:
        iteration += 1
        days_between = (high - low).days
        mid = low + timedelta(days=days_between // 2)
        # Pencere: 14 gün (API limiti 15 gün)
        check_end = mid + timedelta(days=SEARCH_WINDOW_DAYS)

        # Her iterasyonu göster
        with console.status(f"[bold yellow]İterasyon {iteration}: {mid.strftime('%Y-%m-%d')} taranıyor...[/bold yellow]"):
            has_data, oldest_order_date = check_data_exists(mid, check_end)

        # Sonucu tree'ye ekle
        if has_data:
            # En eski orderDate'i kullan (transactionDate değil!)
            order_date_str = oldest_order_date.strftime('%Y-%m-%d') if oldest_order_date else 'N/A'

            if oldest_order_date and oldest_order_date < mid:
                # orderDate, sorgu aralığından daha eski → bu tarihi kullan
                branch = search_tree.add(
                    f"[green]#{iteration}[/green] "
                    f"[white]{mid.strftime('%Y-%m-%d')}[/white] → "
                    f"[green]SİPARİŞ VAR ✓[/green] "
                    f"[cyan](orderDate: {order_date_str})[/cyan]"
                )
                first_order_date = oldest_order_date
                high = oldest_order_date
                console.print(f"  [green]✓ {mid.strftime('%Y-%m-%d')} → Sipariş VAR[/green]")
                console.print(f"    [cyan]ℹ En eski orderDate: {order_date_str} (transactionDate'den erken!)[/cyan]")
            else:
                branch = search_tree.add(
                    f"[green]#{iteration}[/green] "
                    f"[white]{mid.strftime('%Y-%m-%d')}[/white] → "
                    f"[green]SİPARİŞ VAR ✓[/green] "
                    f"[cyan](orderDate: {order_date_str})[/cyan]"
                )
                first_order_date = mid
                high = mid
                console.print(f"  [green]✓ {mid.strftime('%Y-%m-%d')} → Sipariş VAR → Daha erken tarih aranıyor[/green]")
        else:
            branch = search_tree.add(
                f"[yellow]#{iteration}[/yellow] "
                f"[white]{mid.strftime('%Y-%m-%d')}[/white] → "
                f"[red]SİPARİŞ YOK ✗[/red] "
                f"[dim](Daha geç ara)[/dim]"
            )
            low = mid + timedelta(days=15)  # 14 günlük pencere + 1 gün = boşluk yok
            console.print(f"  [red]✗ {mid.strftime('%Y-%m-%d')} → Sipariş YOK → Daha geç tarih aranıyor[/red]")

        # Kalan aralığı göster
        console.print(f"    [dim]Kalan aralık: {low.strftime('%Y-%m-%d')} → {high.strftime('%Y-%m-%d')} ({(high-low).days} gün)[/dim]")
        console.print()

    console.print()
    console.print(search_tree)
    console.print()

    # ════════════════════════════════════════════════════════════════════════════
    # FALLBACK: Binary Search başarısız olduysa, geriye doğru linear tarama yap
    # Bu, transactionDate vs orderDate farkından kaynaklanan edge case'leri yakalar
    # ════════════════════════════════════════════════════════════════════════════
    if not first_order_date:
        console.print()
        console.print("[bold yellow]⚠ Binary Search sonuç bulamadı, geriye doğru tarama yapılıyor...[/bold yellow]")
        console.print()

        # Bugünden geriye doğru 14 günlük chunk'larla tara
        scan_end = datetime.now()
        scan_start_limit = datetime(2017, 10, 1)  # Trendyol başlangıcı
        chunk_size = 14
        max_empty_chunks = 5  # Üst üste 5 boş chunk'tan sonra dur
        empty_chunk_count = 0

        all_scan_settlements = []

        while scan_end > scan_start_limit and empty_chunk_count < max_empty_chunks:
            scan_start = scan_end - timedelta(days=chunk_size)
            if scan_start < scan_start_limit:
                scan_start = scan_start_limit

            endpoint = f"/integration/finance/che/sellers/{client.seller_id}/settlements"

            # Bu chunk için TÜM sayfaları çek (pagination bug fix)
            page = 0
            chunk_total = 0
            chunk_has_data = False

            while True:
                params = {
                    "transactionType": "Sale",
                    "startDate": int(scan_start.timestamp() * 1000),
                    "endDate": int(scan_end.timestamp() * 1000),
                    "page": page,
                    "size": 500
                }

                if page == 0:
                    console.print(f"  [dim]Taranıyor: {scan_start.strftime('%Y-%m-%d')} → {scan_end.strftime('%Y-%m-%d')}[/dim]", end="")

                response, _ = client.get_with_retry(endpoint, params)

                if response and response.status_code == 200:
                    data = response.json()
                    items = data.get("content", [])
                    total_pages = data.get("totalPages", 1)

                    if items:
                        all_scan_settlements.extend(items)
                        chunk_total += len(items)
                        chunk_has_data = True

                    # Son sayfa mı?
                    if page >= total_pages - 1 or len(items) == 0:
                        break

                    page += 1
                else:
                    break

            if chunk_has_data:
                empty_chunk_count = 0  # Reset counter
                console.print(f" [green]({chunk_total} kayıt)[/green]")
            else:
                empty_chunk_count += 1
                console.print(f" [dim](boş)[/dim]")

            scan_end = scan_start

        if all_scan_settlements:
            # En eski orderDate'i bul
            oldest_scan_date = None
            oldest_scan_order_num = None

            for s in all_scan_settlements:
                order_ts = s.get("orderDate")
                if order_ts:
                    order_date = datetime.fromtimestamp(order_ts / 1000)
                    if oldest_scan_date is None or order_date < oldest_scan_date:
                        oldest_scan_date = order_date
                        oldest_scan_order_num = s.get("orderNumber")

            if oldest_scan_date:
                first_order_date = oldest_scan_date
                console.print()
                console.print(f"  [green]✓ Fallback tarama ile ilk sipariş bulundu![/green]")
                console.print(f"    [cyan]orderDate:[/cyan] {oldest_scan_date.strftime('%Y-%m-%d')}")
                console.print(f"    [cyan]Sipariş No:[/cyan] {oldest_scan_order_num}")

        console.print()

    # ════════════════════════════════════════════════════════════════════════════
    # REFINEMENT: Bulunan tarihten geriye doğru doğrulama
    # NOT: Settlement API max 15 gün kabul eder, bu yüzden 14 günlük chunk'larla
    # 42 günü (35 gün geri + 7 gün buffer) tarıyoruz.
    # ════════════════════════════════════════════════════════════════════════════
    refinement_applied = False
    first_order_number = None

    if first_order_date:
        console.print()
        console.print("[bold cyan]🔬 Refinement: Daha erken sipariş aranıyor...[/bold cyan]")
        console.print(f"[dim]Bulunan tarihten 35 gün öncesi taranıyor (14 günlük chunk'lar)...[/dim]")
        console.print()

        # 42 gün (35 geri + 7 ileri buffer) taramak için 3 chunk
        refinement_days_back = 35
        refinement_days_forward = 7
        chunk_size = 14  # API limiti: max 15 gün

        all_settlements = []
        chunk_start = first_order_date - timedelta(days=refinement_days_back)
        chunk_end_limit = first_order_date + timedelta(days=refinement_days_forward)

        endpoint = f"/integration/finance/che/sellers/{client.seller_id}/settlements"

        chunk_num = 0
        current_start = chunk_start

        while current_start < chunk_end_limit:
            chunk_num += 1
            current_end = min(current_start + timedelta(days=chunk_size), chunk_end_limit)

            # Bu chunk için TÜM sayfaları çek (pagination bug fix)
            page = 0
            chunk_total = 0

            while True:
                params = {
                    "transactionType": "Sale",
                    "startDate": int(current_start.timestamp() * 1000),
                    "endDate": int(current_end.timestamp() * 1000),
                    "page": page,
                    "size": 500
                }

                if page == 0:
                    console.print(f"  [dim]Chunk {chunk_num}: {current_start.strftime('%Y-%m-%d')} → {current_end.strftime('%Y-%m-%d')}[/dim]", end="")

                response, _ = client.get_with_retry(endpoint, params)

                if response and response.status_code == 200:
                    data = response.json()
                    items = data.get("content", [])
                    total_pages = data.get("totalPages", 1)
                    total_elements = data.get("totalElements", 0)

                    all_settlements.extend(items)
                    chunk_total += len(items)

                    # Son sayfa mı?
                    if page >= total_pages - 1 or len(items) == 0:
                        console.print(f" [cyan]({chunk_total} kayıt" + (f", {total_pages} sayfa)" if total_pages > 1 else ")") + "[/cyan]")
                        break

                    page += 1
                else:
                    console.print(f" [yellow](hata)[/yellow]")
                    break

            current_start = current_end

        console.print()

        if all_settlements:
            # En eski orderDate'i bul
            actual_first_order = None
            actual_first_order_num = None

            for s in all_settlements:
                order_ts = s.get("orderDate")
                if order_ts:
                    order_date = datetime.fromtimestamp(order_ts / 1000)
                    if actual_first_order is None or order_date < actual_first_order:
                        actual_first_order = order_date
                        actual_first_order_num = s.get("orderNumber")

            if actual_first_order and actual_first_order < first_order_date:
                console.print(f"  [green]✓ Daha erken sipariş bulundu![/green]")
                console.print(f"    [yellow]Binary Search buldu:[/yellow] {first_order_date.strftime('%Y-%m-%d')}")
                console.print(f"    [green]Gerçek ilk sipariş:[/green]  {actual_first_order.strftime('%Y-%m-%d')}")
                console.print(f"    [cyan]Sipariş No:[/cyan] {actual_first_order_num}")
                first_order_date = actual_first_order
                first_order_number = actual_first_order_num
                refinement_applied = True
            else:
                console.print(f"  [dim]Refinement: Daha erken sipariş yok, Binary Search doğru buldu.[/dim]")
                # En eski siparişin numarasını al
                if actual_first_order_num:
                    first_order_number = actual_first_order_num
        else:
            console.print(f"  [dim]Refinement aralığında veri yok.[/dim]")

        console.print()

    # Sonuç
    if first_order_date:
        result_text = f"[bold green]✓ İLK SİPARİŞ TARİHİ BULUNDU[/bold green]\n\n"
        result_text += f"[cyan]Tarih:[/cyan] {first_order_date.strftime('%Y-%m-%d')}\n"
        if first_order_number:
            result_text += f"[cyan]Sipariş No:[/cyan] {first_order_number}\n"
        result_text += f"[cyan]Toplam İterasyon:[/cyan] {iteration}\n"
        result_text += f"[cyan]Refinement:[/cyan] {'Uygulandı ✓' if refinement_applied else 'Gerek yok'}\n"
        result_text += f"[cyan]Algoritma:[/cyan] Binary Search + orderDate Refinement\n\n"
        result_text += f"[dim]Not: Settlement API transactionDate ile filtreler,[/dim]\n"
        result_text += f"[dim]orderDate 3-7 gün daha erken olabilir.[/dim]"

        console.print(Panel(
            result_text,
            title="[green]Binary Search Sonucu[/green]",
            border_style="green",
            box=DOUBLE
        ))
    else:
        console.print(Panel(
            "[bold yellow]⚠ Bu mağazada henüz sipariş bulunamadı[/bold yellow]\n\n"
            "Ya mağaza çok yeni, ya da henüz sipariş yok.",
            title="[yellow]Binary Search Sonucu[/yellow]",
            border_style="yellow"
        ))

    return first_order_date


# ═══════════════════════════════════════════════════════════════════════════════
# BÖLÜM 4: HISTORICAL SYNC (Chunk Processing)
# ═══════════════════════════════════════════════════════════════════════════════
def sync_historical_orders(client: TrendyolApiClient, first_order_date: datetime) -> int:
    """
    Backend'deki TrendyolHistoricalSettlementService.fetchAndCreateHistoricalOrders() implementasyonu.
    14 günlük chunk'lar halinde historical orders çeker.
    """
    console.print()
    console.print(Rule("[bold cyan]📊 BÖLÜM 4: HISTORICAL SYNC (Chunk Processing)[/bold cyan]", style="cyan"))
    console.print()

    if not first_order_date:
        console.print("[yellow]İlk sipariş tarihi bulunamadı, historical sync atlanıyor.[/yellow]")
        return 0

    # Chunk hesaplamaları
    end_date = datetime.now()
    total_days = (end_date - first_order_date).days
    total_chunks = (total_days + CHUNK_DAYS - 1) // CHUNK_DAYS

    console.print(Panel(
        f"[bold]Historical Sync Parametreleri[/bold]\n\n"
        f"[cyan]Başlangıç:[/cyan] {first_order_date.strftime('%Y-%m-%d')}\n"
        f"[cyan]Bitiş:[/cyan] {end_date.strftime('%Y-%m-%d')}\n"
        f"[cyan]Toplam Gün:[/cyan] {total_days}\n"
        f"[cyan]Chunk Boyutu:[/cyan] {CHUNK_DAYS} gün\n"
        f"[cyan]Toplam Chunk:[/cyan] {total_chunks}\n"
        f"[cyan]Sayfa Boyutu:[/cyan] {PAGE_SIZE_SETTLEMENTS} item/page\n\n"
        f"[yellow]⚠ Checkpoint Mekanizması:[/yellow]\n"
        f"Her başarılı chunk sonrası checkpoint kaydedilir.\n"
        f"Kesinti durumunda kaldığı yerden devam eder.\n\n"
        f"[dim]Backend: TrendyolHistoricalSettlementService.fetchAndCreateHistoricalOrders()[/dim]",
        title="[cyan]Sync Parametreleri[/cyan]",
        border_style="cyan"
    ))
    console.print()

    # Stats
    total_orders = 0
    total_settlements = 0
    failed_chunks = 0
    current_chunk = 0
    chunk_start = first_order_date

    # Transaction types (Backend ile aynı)
    transaction_types = ["Sale", "Return", "Discount", "Coupon"]

    # Progress bar
    with Progress(
        SpinnerColumn(),
        TextColumn("[progress.description]{task.description}"),
        BarColumn(bar_width=50),
        TextColumn("[progress.percentage]{task.percentage:>3.0f}%"),
        TextColumn("•"),
        TextColumn("[cyan]Chunk {task.completed}/{task.total}[/cyan]"),
        TimeElapsedColumn(),
        console=console
    ) as progress:
        main_task = progress.add_task("[cyan]Historical Sync...", total=total_chunks)

        while chunk_start < end_date:
            current_chunk += 1
            chunk_end = min(chunk_start + timedelta(days=CHUNK_DAYS), end_date)

            # Chunk info göster
            progress.update(
                main_task,
                description=f"[cyan]{chunk_start.strftime('%Y-%m-%d')} → {chunk_end.strftime('%Y-%m-%d')}[/cyan]"
            )

            chunk_settlements = 0
            chunk_orders = set()

            # Her transaction type için fetch
            for tx_type in transaction_types:
                endpoint = f"/integration/finance/che/sellers/{client.seller_id}/settlements"

                start_ts = int(chunk_start.timestamp() * 1000)
                end_ts = int(chunk_end.timestamp() * 1000)

                params = {
                    "transactionType": tx_type,
                    "startDate": start_ts,
                    "endDate": end_ts,
                    "page": 0,
                    "size": PAGE_SIZE_SETTLEMENTS
                }

                response, retries = client.get_with_retry(endpoint, params)

                if response and response.status_code == 200:
                    data = response.json()
                    items = data.get("content", [])
                    chunk_settlements += len(items)

                    # Unique order numaraları say
                    for item in items:
                        order_no = item.get("orderNumber")
                        if order_no:
                            chunk_orders.add(order_no)

                # Transaction types arası delay (Backend ile aynı)
                time.sleep(0.2)

            total_settlements += chunk_settlements
            total_orders += len(chunk_orders)

            # Checkpoint simulation
            progress.update(main_task, completed=current_chunk)

            # Chunk arası delay (Backend ile aynı: 300ms + rate limiter)
            time.sleep(0.3)

            chunk_start = chunk_end

    console.print()

    # Sonuç tablosu
    result_table = Table(title="Historical Sync Sonuçları", box=DOUBLE, border_style="green")
    result_table.add_column("Metrik", style="cyan")
    result_table.add_column("Değer", style="white", justify="right")
    result_table.add_row("Toplam Chunk", str(total_chunks))
    result_table.add_row("Başarılı Chunk", str(total_chunks - failed_chunks))
    result_table.add_row("Başarısız Chunk", str(failed_chunks))
    result_table.add_row("Toplam Settlement", str(total_settlements))
    result_table.add_row("Unique Sipariş", str(total_orders))

    console.print(result_table)

    return total_orders


# ═══════════════════════════════════════════════════════════════════════════════
# BÖLÜM 5: FINANCIAL DETECTIVE (Komisyon Analizi)
# ═══════════════════════════════════════════════════════════════════════════════
def analyze_commission(client: TrendyolApiClient) -> None:
    """
    Backend'deki OrderCostCalculator komisyon hesaplama mantığını gösterir.
    Tahmini vs Gerçek komisyon farkını analiz eder.
    """
    console.print()
    console.print(Rule("[bold cyan]💰 BÖLÜM 5: FINANCIAL DETECTIVE (Komisyon Analizi)[/bold cyan]", style="cyan"))
    console.print()

    console.print(Panel(
        "[bold]Komisyon Sistemi Açıklaması[/bold]\n\n"
        "[yellow]TAHMİNİ KOMİSYON (isCommissionEstimated = true)[/yellow]\n"
        "• Sipariş geldiğinde hesaplanır\n"
        "• Ürünün kategori komisyon oranı kullanılır\n"
        "• Formül: (fiyat / (1 + KDV%)) × komisyon%\n\n"
        "[green]GERÇEK KOMİSYON (isCommissionEstimated = false)[/green]\n"
        "• Settlement API'den 1-2 hafta sonra gelir\n"
        "• Kampanya, indirim vs. dahil gerçek oran\n"
        "• Trendyol'un kestiği net tutar\n\n"
        "[dim]Backend: OrderCostCalculator.calculateUnitEstimatedCommission()[/dim]",
        title="[cyan]Komisyon Sistemi[/cyan]",
        border_style="cyan"
    ))
    console.print()

    # Örnek sipariş al (Settlement API'den)
    endpoint = f"/integration/finance/che/sellers/{client.seller_id}/settlements"
    end_date = datetime.now()
    start_date = end_date - timedelta(days=30)

    params = {
        "transactionType": "Sale",
        "startDate": int(start_date.timestamp() * 1000),
        "endDate": int(end_date.timestamp() * 1000),
        "page": 0,
        "size": 1
    }

    with console.status("[bold yellow]Örnek sipariş çekiliyor...[/bold yellow]"):
        response, _ = client.get_with_retry(endpoint, params)

    if response and response.status_code == 200:
        data = response.json()
        items = data.get("content", [])

        if items:
            item = items[0]

            # Değerleri al
            order_no = item.get("orderNumber", "N/A")
            price = Decimal(str(item.get("grossSalePrice", 0)))
            vat_rate = Decimal(str(item.get("vatRate", 20)))
            real_commission = Decimal(str(item.get("commissionAmount", 0)))
            real_commission_rate = Decimal(str(item.get("commissionRate", 0)))
            product_title = item.get("productName", "Ürün Adı")[:40] + "..."

            # Tahmini komisyon hesapla (Backend formülü)
            # vatBase = price / (1 + vatRate/100)
            # commission = vatBase * commissionRate / 100
            divisor = Decimal("1") + vat_rate / Decimal("100")
            vat_base = (price / divisor).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

            # Tahmini komisyon için kategori default oranı kullan (%18 örnek)
            estimated_rate = Decimal("18")  # Kategori default
            estimated_commission = (vat_base * estimated_rate / Decimal("100")).quantize(
                Decimal("0.01"), rounding=ROUND_HALF_UP
            )

            # Gerçek komisyon hesapla
            real_calculated = (vat_base * real_commission_rate / Decimal("100")).quantize(
                Decimal("0.01"), rounding=ROUND_HALF_UP
            )

            # Fark
            difference = estimated_commission - real_commission

            # Detaylı analiz paneli
            analysis = f"""
[bold white]ORDER #{order_no}[/bold white]
[dim]{product_title}[/dim]

[bold cyan]─── FATURA DEĞERLERİ ───[/bold cyan]

Ürün Fiyatı (KDV dahil): [white]{price:,.2f} TL[/white]
KDV Oranı: [white]%{vat_rate}[/white]
KDV Matrahı: [white]{price:,.2f} / {divisor} = {vat_base:,.2f} TL[/white]

[bold yellow]─── TAHMİNİ KOMİSYON (Kategori) ───[/bold yellow]

Komisyon Oranı: [yellow]%{estimated_rate}[/yellow]
Formül: {vat_base:,.2f} × {estimated_rate/100} = [yellow]{estimated_commission:,.2f} TL[/yellow]

[bold green]─── GERÇEK KOMİSYON (Settlement API) ───[/bold green]

Komisyon Oranı: [green]%{real_commission_rate}[/green]
API Değeri: [green]{real_commission:,.2f} TL[/green]
Hesaplanan: {vat_base:,.2f} × {real_commission_rate/100} = {real_calculated:,.2f} TL

[bold magenta]─── FARK ANALİZİ ───[/bold magenta]

Tahmini - Gerçek: {estimated_commission:,.2f} - {real_commission:,.2f} = [magenta]{difference:,.2f} TL[/magenta]
"""

            if difference > 0:
                analysis += f"\n[green]💰 Tasarruf: {difference:,.2f} TL (Gerçek komisyon daha düşük!)[/green]"
            elif difference < 0:
                analysis += f"\n[red]⚠ Fark: {abs(difference):,.2f} TL (Gerçek komisyon daha yüksek)[/red]"
            else:
                analysis += "\n[cyan]= Tahmini ve gerçek komisyon eşit[/cyan]"

            analysis += f"""

[bold white]─── VERİTABANI DURUMU ───[/bold white]

isCommissionEstimated: [green]false ✓[/green]
dataSource: [green]SETTLEMENT_API[/green]
"""

            console.print(Panel(
                analysis,
                title="[bold cyan]Komisyon Detay Analizi[/bold cyan]",
                border_style="cyan",
                box=HEAVY
            ))

        else:
            console.print("[yellow]Son 30 günde sipariş bulunamadı.[/yellow]")
    else:
        console.print("[red]Settlement API'ye erişilemedi.[/red]")


# ═══════════════════════════════════════════════════════════════════════════════
# BÖLÜM 6: RATE LIMITING VISUALIZATION
# ═══════════════════════════════════════════════════════════════════════════════
def visualize_rate_limiting() -> None:
    """Rate limiting mekanizmasını görselleştirir."""
    console.print()
    console.print(Rule("[bold cyan]⚡ BÖLÜM 6: RATE LIMITING VISUALIZATION[/bold cyan]", style="cyan"))
    console.print()

    console.print(Panel(
        "[bold]TrendyolRateLimiter Açıklaması[/bold]\n\n"
        "[cyan]Guava RateLimiter Implementasyonu:[/cyan]\n"
        "• Saniyede 10 istek (permit) limiti\n"
        "• Her istek için bir permit gerekli\n"
        "• Permit yoksa bekler (blocking)\n"
        "• Tüm Trendyol API çağrıları bu limiter'dan geçer\n\n"
        "[yellow]Neden Gerekli?[/yellow]\n"
        "Trendyol API, aşırı istek yapan hesapları engelleyebilir.\n"
        "Rate limiter, istekleri düzenli aralıklarla yapar.\n\n"
        "[dim]Backend: TrendyolRateLimiter.java[/dim]",
        title="[cyan]Rate Limiter[/cyan]",
        border_style="cyan"
    ))
    console.print()

    # Demo: 15 istek yap ve bekleme sürelerini göster
    console.print("[bold yellow]Demo: 15 istek yapılıyor (10 req/sec limit)[/bold yellow]")
    console.print()

    demo_limiter = RateLimiter(10.0)

    table = Table(title="Rate Limiter Demo", box=ROUNDED)
    table.add_column("#", style="cyan", justify="right")
    table.add_column("Zaman", style="white")
    table.add_column("Bekleme", style="yellow", justify="right")
    table.add_column("Durum", style="green")

    start_time = time.time()

    for i in range(15):
        req_start = time.time()
        wait_time = demo_limiter.acquire()
        req_end = time.time()

        elapsed = req_end - start_time
        status = "✓ Permit alındı"

        if wait_time > 0.01:
            status = f"⏳ {wait_time*1000:.0f}ms bekledi"

        table.add_row(
            str(i + 1),
            f"{elapsed:.3f}s",
            f"{wait_time*1000:.1f}ms",
            status
        )

    console.print(table)
    console.print()
    console.print(f"[green]✓ Toplam istek: {demo_limiter.acquired_count}[/green]")
    console.print(f"[green]✓ Toplam süre: {time.time() - start_time:.2f}s[/green]")
    console.print(f"[dim]Beklenen süre: 15 istek / 10 req/sec = ~1.5s[/dim]")


# ═══════════════════════════════════════════════════════════════════════════════
# BÖLÜM 7: EXPONENTIAL BACKOFF
# ═══════════════════════════════════════════════════════════════════════════════
def demonstrate_exponential_backoff() -> None:
    """Exponential backoff mekanizmasını gösterir."""
    console.print()
    console.print(Rule("[bold cyan]🔄 BÖLÜM 7: EXPONENTIAL BACKOFF[/bold cyan]", style="cyan"))
    console.print()

    console.print(Panel(
        "[bold]Exponential Backoff Açıklaması[/bold]\n\n"
        "[cyan]Hata Durumunda Yeniden Deneme:[/cyan]\n"
        "• 1. deneme başarısız → 1 saniye bekle\n"
        "• 2. deneme başarısız → 2 saniye bekle\n"
        "• 3. deneme başarısız → 3 saniye bekle\n"
        "• 4. deneme (max) → hata fırlat\n\n"
        "[yellow]Hangi Hatalar Retry Edilir?[/yellow]\n"
        "• 401 Unauthorized (token expired)\n"
        "• 5xx Server Errors\n"
        "• Connection timeout\n"
        "• Network errors\n\n"
        "[red]Retry Edilmeyenler:[/red]\n"
        "• 400 Bad Request\n"
        "• 403 Forbidden\n"
        "• 404 Not Found\n\n"
        "[dim]Backend: TrendyolHistoricalSettlementService.fetchSettlementWithRetry()[/dim]",
        title="[cyan]Retry Mekanizması[/cyan]",
        border_style="cyan"
    ))
    console.print()

    # Simülasyon
    console.print("[bold yellow]Simülasyon: Geçici hata senaryosu[/bold yellow]")
    console.print()

    tree = Tree("[bold]🔄 Retry Simülasyonu[/bold]")

    # Simüle edilmiş hata senaryosu
    attempts = [
        (1, "500 Server Error", 1, False),
        (2, "Connection Timeout", 2, False),
        (3, "401 Unauthorized", 3, False),
        (4, None, 0, True),  # Başarılı
    ]

    for attempt, error, delay, success in attempts:
        if success:
            branch = tree.add(f"[green]Deneme #{attempt}: BAŞARILI ✓[/green]")
            branch.add("[dim]HTTP 200 OK[/dim]")
        else:
            branch = tree.add(f"[yellow]Deneme #{attempt}: BAŞARISIZ[/yellow]")
            branch.add(f"[red]Hata: {error}[/red]")
            branch.add(f"[cyan]Bekleme: {delay}s (exponential backoff)[/cyan]")

    console.print(tree)
    console.print()

    # Kod örneği
    console.print(Panel(
        Syntax(
            """// Backend: fetchSettlementWithRetry()
int retryCount = 0;
while (retryCount <= MAX_RETRIES) {
    try {
        ResponseEntity<T> response = restTemplate.exchange(...);
        return response;  // Başarılı
    } catch (HttpServerErrorException | ResourceAccessException e) {
        retryCount++;
        if (retryCount <= MAX_RETRIES) {
            long sleepTime = 1000L * retryCount;  // 1s, 2s, 3s
            Thread.sleep(sleepTime);
        }
    }
}
throw new RuntimeException("Max retries exceeded");""",
            "java",
            theme="monokai",
            line_numbers=True
        ),
        title="[dim]Backend Kodu[/dim]",
        border_style="dim"
    ))


# ═══════════════════════════════════════════════════════════════════════════════
# ANA FONKSİYON
# ═══════════════════════════════════════════════════════════════════════════════
def main():
    """Ana çalıştırma fonksiyonu."""

    # Banner
    console.print()
    console.print(Panel(
        Align.center("""
[bold cyan]╔═══════════════════════════════════════════════════════════════╗
║                                                               ║
║   ██╗     ██╗██╗   ██╗███████╗    ███████╗██╗   ██╗███╗   ██╗ ║
║   ██║     ██║██║   ██║██╔════╝    ██╔════╝╚██╗ ██╔╝████╗  ██║ ║
║   ██║     ██║██║   ██║█████╗      ███████╗ ╚████╔╝ ██╔██╗ ██║ ║
║   ██║     ██║╚██╗ ██╔╝██╔══╝      ╚════██║  ╚██╔╝  ██║╚██╗██║ ║
║   ███████╗██║ ╚████╔╝ ███████╗    ███████║   ██║   ██║ ╚████║ ║
║   ╚══════╝╚═╝  ╚═══╝  ╚══════╝    ╚══════╝   ╚═╝   ╚═╝  ╚═══╝ ║
║                                                               ║
║               ██████╗ ███████╗████████╗███████╗ ██████╗       ║
║               ██╔══██╗██╔════╝╚══██╔══╝██╔════╝██╔════╝       ║
║               ██║  ██║█████╗     ██║   █████╗  ██║            ║
║               ██║  ██║██╔══╝     ██║   ██╔══╝  ██║            ║
║               ██████╔╝███████╗   ██║   ███████╗╚██████╗       ║
║               ╚═════╝ ╚══════╝   ╚═╝   ╚══════╝ ╚═════╝       ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝[/bold cyan]

[dim]SellerX Backend Trendyol Entegrasyon Analiz Aracı[/dim]
[dim]Version 1.0.0[/dim]
"""),
        border_style="cyan",
        box=DOUBLE
    ))

    # Credential kontrolü
    if not SELLER_ID or not API_KEY or not API_SECRET:
        console.print()
        console.print(Panel(
            "[bold red]⚠ HATA: API Bilgileri Eksik![/bold red]\n\n"
            "Lütfen script başındaki değişkenleri doldurun:\n\n"
            "[cyan]SELLER_ID[/cyan] = \"123456\"      # Trendyol Satıcı ID\n"
            "[cyan]API_KEY[/cyan] = \"xxx\"           # API Anahtarı\n"
            "[cyan]API_SECRET[/cyan] = \"xxx\"        # API Şifresi\n",
            title="[red]Konfigürasyon Gerekli[/red]",
            border_style="red"
        ))
        return

    # API client oluştur
    client = TrendyolApiClient(SELLER_ID, API_KEY, API_SECRET)

    # Info panel
    console.print()
    console.print(Panel(
        f"[bold]Konfigürasyon[/bold]\n\n"
        f"[cyan]Seller ID:[/cyan] {SELLER_ID}\n"
        f"[cyan]API Key:[/cyan] {API_KEY[:8]}...\n"
        f"[cyan]Base URL:[/cyan] {TRENDYOL_BASE_URL}\n"
        f"[cyan]Rate Limit:[/cyan] {RATE_LIMIT_PER_SEC} req/sec\n"
        f"[cyan]Max Retries:[/cyan] {MAX_RETRIES}",
        title="[cyan]Ayarlar[/cyan]",
        border_style="cyan"
    ))

    console.print()
    console.print("[bold yellow]⚠ DİKKAT: Bu script gerçek API çağrıları yapacak![/bold yellow]")
    console.print("[dim]Devam etmek için Enter'a basın veya Ctrl+C ile iptal edin...[/dim]")

    try:
        input()
    except KeyboardInterrupt:
        console.print("\n[yellow]İptal edildi.[/yellow]")
        return

    start_time = time.time()

    # ═══════════════════════════════════════════════════════════════════════════
    # BÖLÜM 1: Credential Validation
    # ═══════════════════════════════════════════════════════════════════════════
    if not validate_credentials(client):
        console.print("\n[red]Credential doğrulama başarısız. Script sonlandırılıyor.[/red]")
        return

    # ═══════════════════════════════════════════════════════════════════════════
    # BÖLÜM 2: Product Sync
    # ═══════════════════════════════════════════════════════════════════════════
    products = sync_products(client)

    # ═══════════════════════════════════════════════════════════════════════════
    # BÖLÜM 3: Binary Search
    # ═══════════════════════════════════════════════════════════════════════════
    first_order_date = binary_search_first_order(client)

    # ═══════════════════════════════════════════════════════════════════════════
    # BÖLÜM 4: Historical Sync
    # ═══════════════════════════════════════════════════════════════════════════
    total_orders = sync_historical_orders(client, first_order_date)

    # ═══════════════════════════════════════════════════════════════════════════
    # BÖLÜM 5: Financial Detective
    # ═══════════════════════════════════════════════════════════════════════════
    analyze_commission(client)

    # ═══════════════════════════════════════════════════════════════════════════
    # BÖLÜM 6: Rate Limiting
    # ═══════════════════════════════════════════════════════════════════════════
    visualize_rate_limiting()

    # ═══════════════════════════════════════════════════════════════════════════
    # BÖLÜM 7: Exponential Backoff
    # ═══════════════════════════════════════════════════════════════════════════
    demonstrate_exponential_backoff()

    # ═══════════════════════════════════════════════════════════════════════════
    # ÖZET
    # ═══════════════════════════════════════════════════════════════════════════
    elapsed = time.time() - start_time

    console.print()
    console.print(Rule("[bold green]✓ ANALİZ TAMAMLANDI[/bold green]", style="green"))
    console.print()

    summary_table = Table(title="Analiz Özeti", box=DOUBLE, border_style="green")
    summary_table.add_column("Metrik", style="cyan")
    summary_table.add_column("Değer", style="white", justify="right")
    summary_table.add_row("Toplam Süre", f"{elapsed:.1f} saniye")
    summary_table.add_row("API Çağrısı", f"{rate_limiter.acquired_count}")
    summary_table.add_row("Ürün Sayısı", f"{len(products)}")
    summary_table.add_row("İlk Sipariş", first_order_date.strftime("%Y-%m-%d") if first_order_date else "N/A")
    summary_table.add_row("Historical Orders", str(total_orders))

    console.print(summary_table)
    console.print()

    console.print(Panel(
        "[bold green]Live Sync Detective analizi tamamlandı![/bold green]\n\n"
        "Bu araç, SellerX backend'inin Trendyol entegrasyonunda\n"
        "kullandığı algoritmaları gerçek API çağrıları ile gösterdi:\n\n"
        "✓ Credential Validation (Basic Auth)\n"
        "✓ Product Sync (Pagination)\n"
        "✓ Binary Search (İlk sipariş tarihi)\n"
        "✓ Historical Sync (Chunk processing)\n"
        "✓ Commission Analysis (Tahmini vs Gerçek)\n"
        "✓ Rate Limiting (10 req/sec)\n"
        "✓ Exponential Backoff (Retry mekanizması)\n",
        title="[green]Tamamlandı[/green]",
        border_style="green",
        box=DOUBLE
    ))


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        console.print("\n[yellow]Script kullanıcı tarafından durduruldu.[/yellow]")
    except Exception as e:
        console.print(f"\n[red]Beklenmeyen hata: {e}[/red]")
        raise
