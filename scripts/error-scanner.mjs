#!/usr/bin/env node
/**
 * ============================================================
 *  网页错误扫描器 (Web Error Scanner)
 *  自动检测前端 JS 异常、资源加载失败、HTTP 状态码错误
 * ============================================================
 *
 * 用法:
 *   node error-scanner.mjs [options] <url1> [url2 ...]
 *
 * 选项:
 *   --headed              有头模式（显示浏览器窗口）
 *   --timeout <ms>        页面加载超时（默认 30000ms）
 *   --wait <ms>           页面加载后额外等待（默认 2000ms）
 *   --output <file>       输出 JSON 报告文件（默认 error-report.json）
 *   --urls <file>         从文件读取 URL 列表（每行一个）
 *   --screenshots         对每个页面截图保存
 *   --mobile              模拟移动端（iPhone 12）
 *   --depth <n>           爬取深度（自动提取同源链接，默认 0 不爬取）
 *   --help                显示帮助
 *
 * 示例:
 *   node error-scanner.mjs http://127.0.0.1:5174
 *   node error-scanner.mjs --headed --screenshots http://127.0.0.1:5174 http://127.0.0.1:5176
 *   node error-scanner.mjs --urls urls.txt --output report.json
 */

import { chromium } from 'playwright';
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { createInterface } from 'readline';

const __dirname = dirname(fileURLToPath(import.meta.url));

// ==================== 配置解析 ====================

function parseArgs() {
  const args = process.argv.slice(2);
  const config = {
    urls: [],
    headed: false,
    timeout: 30000,
    wait: 2000,
    output: 'error-report.json',
    urlsFile: null,
    screenshots: false,
    mobile: false,
    depth: 0,
    help: false,
  };

  let i = 0;
  while (i < args.length) {
    switch (args[i]) {
      case '--headed':          config.headed = true; break;
      case '--screenshots':     config.screenshots = true; break;
      case '--mobile':          config.mobile = true; break;
      case '--help':            config.help = true; break;
      case '--timeout':         config.timeout = parseInt(args[++i]) || 30000; break;
      case '--wait':            config.wait = parseInt(args[++i]) || 2000; break;
      case '--output':          config.output = args[++i] || 'error-report.json'; break;
      case '--urls':            config.urlsFile = args[++i]; break;
      case '--depth':           config.depth = parseInt(args[++i]) || 0; break;
      default:
        if (args[i].startsWith('--')) {
          console.warn(`⚠ 未知选项: ${args[i]}`);
        } else {
          config.urls.push(args[i]);
        }
    }
    i++;
  }

  return config;
}

function showHelp() {
  console.log(`
╔══════════════════════════════════════════════════════════╗
║           🔍 网页错误扫描器  Web Error Scanner           ║
╠══════════════════════════════════════════════════════════╣
║  用法: node error-scanner.mjs [options] <url1> [url2..] ║
╠══════════════════════════════════════════════════════════╣
║  选项:                                                   ║
║    --headed         有头模式（显示浏览器）               ║
║    --timeout <ms>   页面加载超时 (默认30000)             ║
║    --wait <ms>      加载后等待毫秒 (默认2000)            ║
║    --output <file>  输出文件 (默认 error-report.json)    ║
║    --urls <file>    从文件读取URL列表                    ║
║    --screenshots    保存页面截图                         ║
║    --mobile         模拟移动端 iPhone 12                 ║
║    --depth <n>      爬取同源链接深度 (默认0)             ║
║    --help           显示此帮助                           ║
╚══════════════════════════════════════════════════════════╝
`);
}

// ==================== 错误收集器 ====================

class ErrorCollector {
  constructor() {
    this.consoleErrors = [];
    this.pageErrors = [];
    this.resourceErrors = [];
    this.httpErrors = [];
    this.warnings = [];
  }

  /** JS console.error / console.warn */
  addConsole(msg) {
    // console.error or explicit error-level
    if (msg.type() === 'error') {
      this.consoleErrors.push({
        type: 'CONSOLE_ERROR',
        severity: 'HIGH',
        text: msg.text(),
        location: msg.location() || {},
        timestamp: new Date().toISOString(),
      });
    } else if (msg.type() === 'warning') {
      this.warnings.push({
        type: 'CONSOLE_WARNING',
        severity: 'LOW',
        text: msg.text(),
        location: msg.location() || {},
        timestamp: new Date().toISOString(),
      });
    }
  }

  /** 未捕获的 JS 异常 */
  addPageError(error) {
    this.pageErrors.push({
      type: 'UNCAUGHT_EXCEPTION',
      severity: 'CRITICAL',
      message: error.message,
      stack: error.stack || null,
      timestamp: new Date().toISOString(),
    });
  }

  /** 资源加载失败（img, script, css, font等） */
  addResourceError(request, failureText) {
    this.resourceErrors.push({
      type: 'RESOURCE_FAILURE',
      severity: this._resourceSeverity(request),
      url: request.url(),
      resourceType: request.resourceType(),
      failure: failureText,
      timestamp: new Date().toISOString(),
    });
  }

  /** HTTP 4xx/5xx 响应 */
  addHttpError(response) {
    const url = response.url();
    const status = response.status();
    // 只关注 API 请求的 HTTP 错误，静态资源用 resource error 捕获
    if (status >= 400 && !url.match(/\.(png|jpg|jpeg|gif|svg|webp|ico|css|js|woff2?|ttf|eot)(\?|$)/i)) {
      this.httpErrors.push({
        type: 'HTTP_ERROR',
        severity: status >= 500 ? 'CRITICAL' : 'HIGH',
        url,
        statusCode: status,
        statusText: response.statusText(),
        method: response.request().method(),
        timestamp: new Date().toISOString(),
      });
    }
  }

  _resourceSeverity(request) {
    const type = request.resourceType();
    // 核心资源失败 = 高严重性
    if (['script', 'stylesheet', 'document', 'xhr', 'fetch'].includes(type)) return 'CRITICAL';
    if (type === 'image') return 'MEDIUM';
    if (['font', 'media'].includes(type)) return 'LOW';
    return 'MEDIUM';
  }

  /** 获取汇总结果 */
  summary() {
    const all = [
      ...this.pageErrors,
      ...this.consoleErrors,
      ...this.resourceErrors,
      ...this.httpErrors,
    ];
    const bySeverity = (s) => all.filter(e => e.severity === s).length;

    return {
      totalErrors: all.length,
      critical: bySeverity('CRITICAL'),
      high: bySeverity('HIGH'),
      medium: bySeverity('MEDIUM'),
      low: bySeverity('LOW') + this.warnings.length,
      warnings: this.warnings.length,
    };
  }

  /** 获取完整报告 */
  report(url, duration) {
    return {
      url,
      scanTime: new Date().toISOString(),
      duration: `${duration}ms`,
      summary: this.summary(),
      errors: {
        uncaughtExceptions: this.pageErrors,
        consoleErrors: this.consoleErrors,
        resourceFailures: this.resourceErrors,
        httpErrors: this.httpErrors,
      },
      warnings: this.warnings,
    };
  }
}

// ==================== 页面扫描器 ====================

async function scanPage(browser, pageUrl, config) {
  const context = await browser.newContext({
    viewport: config.mobile
      ? { width: 390, height: 844 }
      : { width: 1440, height: 900 },
    userAgent: config.mobile
      ? 'Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15'
      : undefined,
    ignoreHTTPSErrors: true,
  });

  const page = await context.newPage();
  const collector = new ErrorCollector();

  // 监听 console 消息
  page.on('console', msg => collector.addConsole(msg));

  // 监听未捕获异常
  page.on('pageerror', error => collector.addPageError(error));

  // 监听请求失败（资源加载失败）
  page.on('requestfailed', request => {
    collector.addResourceError(request, request.failure()?.errorText || 'Unknown');
  });

  // 监听 HTTP 响应错误
  page.on('response', response => {
    if (response.status() >= 400) {
      collector.addHttpError(response);
    }
  });

  const startTime = Date.now();

  try {
    console.log(`  🌐 加载: ${pageUrl}`);
    await page.goto(pageUrl, {
      waitUntil: 'networkidle',
      timeout: config.timeout,
    });

    // 额外等待异步内容加载
    await page.waitForTimeout(config.wait);

    // 滚动触发懒加载
    await page.evaluate(() => {
      window.scrollTo(0, document.body.scrollHeight / 2);
      return new Promise(r => setTimeout(r, 500));
    });
    await page.evaluate(() => {
      window.scrollTo(0, document.body.scrollHeight);
      return new Promise(r => setTimeout(r, 500));
    });

    // 截图
    if (config.screenshots) {
      const dir = `${__dirname}/screenshots`;
      if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
      const filename = pageUrl.replace(/[^a-zA-Z0-9]/g, '_').slice(0, 60);
      await page.screenshot({ path: `${dir}/${filename}.png`, fullPage: true });
      console.log(`  📸 截图: ${filename}.png`);
    }

    // 提取同源链接（爬取模式）
    let discoveredUrls = [];
    if (config.depth > 0) {
      const urlObj = new URL(pageUrl);
      discoveredUrls = await page.evaluate((origin) => {
        return Array.from(document.querySelectorAll('a[href]'))
          .map(a => {
            try { return new URL(a.href, origin).href; } catch { return null; }
          })
          .filter(u => u && u.startsWith(origin) && !u.includes('#'));
      }, urlObj.origin);
    }

    const elapsed = Date.now() - startTime;
    const report = collector.report(pageUrl, elapsed);
    return { report, discoveredUrls };

  } catch (error) {
    console.error(`  ❌ 加载失败: ${error.message}`);
    const elapsed = Date.now() - startTime;
    collector.pageErrors.push({
      type: 'NAVIGATION_FAILURE',
      severity: 'CRITICAL',
      message: error.message,
      timestamp: new Date().toISOString(),
    });
    return { report: collector.report(pageUrl, elapsed), discoveredUrls: [] };
  } finally {
    await context.close();
  }
}

// ==================== 主流程 ====================

async function main() {
  const config = parseArgs();

  if (config.help) {
    showHelp();
    process.exit(0);
  }

  // 收集 URL
  if (config.urlsFile) {
    const content = readFileSync(config.urlsFile, 'utf-8');
    config.urls.push(...content.split('\n').map(l => l.trim()).filter(Boolean));
  }

  if (config.urls.length === 0) {
    console.log('❌ 请提供至少一个 URL，或使用 --help 查看用法');
    process.exit(1);
  }

  console.log(`
╔══════════════════════════════════════════════╗
║    🔍 网页错误扫描器                           ║
╠══════════════════════════════════════════════╣
║  目标页面: ${String(config.urls.length).padEnd(35)}║
║  模式: ${(config.headed ? '有头' : '无头').padEnd(39)}║
║  超时: ${String(config.timeout + 'ms').padEnd(39)}║
║  设备: ${(config.mobile ? 'iPhone 12' : 'Desktop 1440x900').padEnd(39)}║
╚══════════════════════════════════════════════╝
`);

  // 启动浏览器
  const browser = await chromium.launch({
    headless: !config.headed,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });

  const allReports = [];
  const scannedUrls = new Set();
  const queue = [...config.urls];

  try {
    while (queue.length > 0 && scannedUrls.size < 50) { // 最多扫描50个页面
      const url = queue.shift();
      if (scannedUrls.has(url)) continue;
      scannedUrls.add(url);

      const { report, discoveredUrls } = await scanPage(browser, url, config);
      allReports.push(report);

      // 爬取模式：将发现的同源链接加入队列
      if (config.depth > 0 && discoveredUrls.length > 0) {
        const depth = parseInt(url.match(/depth=(\d+)/)?.[1] || '0') + 1;
        if (depth <= config.depth) {
          queue.push(...discoveredUrls.map(u => `${u}${u.includes('?') ? '&' : '?'}depth=${depth}`));
        }
      }
    }
  } finally {
    await browser.close();
  }

  // 生成汇总
  const totalSummary = {
    scannedPages: allReports.length,
    totalErrors: allReports.reduce((s, r) => s + r.summary.totalErrors, 0),
    critical: allReports.reduce((s, r) => s + r.summary.critical, 0),
    high: allReports.reduce((s, r) => s + r.summary.high, 0),
    medium: allReports.reduce((s, r) => s + r.summary.medium, 0),
    low: allReports.reduce((s, r) => s + r.summary.low, 0),
    totalWarnings: allReports.reduce((s, r) => s + r.summary.warnings, 0),
  };

  const output = {
    scanMetadata: {
      scanner: 'Web Error Scanner v1.0',
      timestamp: new Date().toISOString(),
      mode: config.headed ? 'headed' : 'headless',
      device: config.mobile ? 'mobile-iPhone12' : 'desktop',
      timeout: config.timeout,
    },
    summary: totalSummary,
    pages: allReports,
  };

  // 输出 JSON 报告
  writeFileSync(config.output, JSON.stringify(output, null, 2));
  console.log(`\n📄 报告已保存: ${resolve(config.output)}`);

  // 控制台汇总
  console.log(`
╔══════════════════════════════════════════════╗
║              📊 扫描汇总                       ║
╠══════════════════════════════════════════════╣
║  扫描页面: ${String(totalSummary.scannedPages).padEnd(36)}║
║  总错误数: ${String(totalSummary.totalErrors).padEnd(36)}║
╠══════════════════════════════════════════════╣
║  ⛔ CRITICAL: ${String(totalSummary.critical).padEnd(34)}║
║  🔴 HIGH:     ${String(totalSummary.high).padEnd(34)}║
║  🟡 MEDIUM:   ${String(totalSummary.medium).padEnd(34)}║
║  ⚪ LOW:      ${String(totalSummary.low).padEnd(34)}║
║  ⚠  WARNINGS: ${String(totalSummary.totalWarnings).padEnd(34)}║
╚══════════════════════════════════════════════╝
`);

  // 如果有严重错误，列出前5条
  if (totalSummary.critical > 0 || totalSummary.high > 0) {
    console.log('⚠ 高严重性错误详情（前5条）:\n');
    let count = 0;
    for (const page of allReports) {
      const hi = [
        ...page.errors.uncaughtExceptions,
        ...page.errors.consoleErrors,
        ...page.errors.resourceFailures.filter(e => e.severity === 'CRITICAL' || e.severity === 'HIGH'),
        ...page.errors.httpErrors,
      ];
      for (const err of hi) {
        if (count++ >= 5) break;
        const label = {
          UNCAUGHT_EXCEPTION: '⛔',
          CONSOLE_ERROR: '🔴',
          RESOURCE_FAILURE: '🟡',
          HTTP_ERROR: '🔴',
        }[err.type] || '⚠';
        console.log(`  ${label} [${err.severity}] ${page.url}`);
        console.log(`     ${err.type}: ${err.message || err.text || err.url}`);
        if (err.statusCode) console.log(`     HTTP ${err.statusCode} ${err.statusText}`);
        console.log();
      }
      if (count >= 5) break;
    }
  }

  return totalSummary.totalErrors;
}

main()
  .then(errors => process.exit(errors > 0 ? 1 : 0))
  .catch(err => {
    console.error('扫描器异常:', err);
    process.exit(2);
  });
