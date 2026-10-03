import AxeBuilder from '@axe-core/playwright'
import { expect, test, type Page } from '@playwright/test'

// Contas de demonstração do perfil local (src/main/resources/application-local.yml).
const PASSWORD = process.env.E2E_PASSWORD ?? 'workflow-demo-2026'
const SCREENSHOTS = process.env.E2E_SCREENSHOTS

async function login(page: Page, username: string) {
  await page.goto('/login')
  await page.getByLabel('Usuário').fill(username)
  await page.getByLabel('Senha').fill(PASSWORD)
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page).toHaveURL(/\/dashboard$/)
}

async function logout(page: Page) {
  await page.getByRole('button', { name: 'Sair' }).first().click()
  await expect(page).toHaveURL(/\/login/)
}

async function snap(page: Page, name: string) {
  if (SCREENSHOTS) {
    await page.screenshot({ path: `${SCREENSHOTS}/${name}.png`, fullPage: true })
  }
}

async function expectNoAccessibilityViolations(page: Page) {
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze()
  const violations = results.violations.map(
    (violation) => `${violation.id}: ${violation.nodes.map((node) => node.target.join(' ')).join(' | ')}`,
  )
  expect(violations).toEqual([])
}

test.describe.configure({ mode: 'serial' })

const definitionKey = `purchase-e2e-${Date.now()}`
let workflowUrl = ''

test('rejects invalid credentials without revealing which part is wrong', async ({ page }) => {
  await page.goto('/workflows')
  await expect(page).toHaveURL(/\/login\?redirect=%2Fworkflows/)
  await expectNoAccessibilityViolations(page)
  await snap(page, '01-login')

  await page.getByLabel('Usuário').fill('admin')
  await page.getByLabel('Senha').fill('wrong-password')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page.getByRole('alert')).toContainText('Usuário ou senha inválidos.')
})

test('administrator models and activates a process', async ({ page }) => {
  await login(page, 'admin')
  await expectNoAccessibilityViolations(page)
  await snap(page, '02-dashboard')

  await page.getByRole('link', { name: 'Definições' }).first().click()
  await page.getByRole('link', { name: 'Nova definição' }).click()
  await page.getByRole('button', { name: 'Usar exemplo' }).click()
  await page.getByLabel('Chave').fill(definitionKey)
  await page.getByLabel('Nome', { exact: true }).fill('Aprovação de compras (E2E)')
  await snap(page, '03-definition-editor')
  await page.getByRole('button', { name: 'Criar definição' }).click()

  await expect(page).toHaveURL(new RegExp(`/definitions/${definitionKey}\\?version=1`))
  await expect(page.getByRole('img', { name: /Diagrama do processo com 5 estados e 6 transições/ })).toBeVisible()
  await page.getByRole('button', { name: 'Ativar' }).click()
  await expect(page.getByText('Versão 1 ativada.')).toBeVisible()
  await expectNoAccessibilityViolations(page)
  await snap(page, '04-definition-detail')
  await logout(page)
})

test('requester creates and submits a purchase above the limit', async ({ page }) => {
  await login(page, 'user')
  await page.getByRole('link', { name: 'Nova solicitação' }).click()
  const dialog = page.getByRole('dialog', { name: 'Nova solicitação' })
  await dialog.getByLabel('Processo').selectOption(definitionKey)
  await dialog.getByLabel('Valor da variável 1').fill('15000')
  await snap(page, '05-new-request')
  await dialog.getByRole('button', { name: 'Criar workflow' }).click()

  await expect(page).toHaveURL(/\/workflows\/[0-9a-f-]{36}$/)
  workflowUrl = new URL(page.url()).pathname
  await page.getByRole('button', { name: 'Iniciar' }).click()
  await expect(page.getByText('Workflow iniciado.')).toBeVisible()
  await page.getByRole('button', { name: 'submit' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar' }).click()
  await expect(page.getByText(/Novo estado: PENDING_APPROVAL/)).toBeVisible()

  // Usuário comum vê as ações do gestor, mas não pode executá-las.
  await expect(page.getByRole('button', { name: 'approve' })).toBeDisabled()
  await logout(page)
})

test('manager is stopped by the business rule and forwards to finance', async ({ page }) => {
  await login(page, 'manager')
  await page.goto(workflowUrl)
  await page.getByRole('button', { name: 'approve' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog.getByText('amount menor ou igual a 10000')).toBeVisible()
  await dialog.getByRole('button', { name: 'Confirmar' }).click()
  await expect(dialog.getByRole('alert')).toContainText('As regras desta ação não foram atendidas.')
  await snap(page, '06-rule-not-satisfied')
  await dialog.getByRole('button', { name: 'Fechar' }).click()

  await page.getByRole('button', { name: 'send-to-finance' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar' }).click()
  await expect(page.getByText(/Novo estado: FINANCIAL_VALIDATION/)).toBeVisible()
  await logout(page)
})

test('finance approves and the history tells the whole story', async ({ page }) => {
  await login(page, 'finance')
  await page.goto(workflowUrl)
  await page.getByRole('button', { name: 'approve' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar' }).click()

  await expect(page.getByText('Concluído').first()).toBeVisible()
  const history = page.getByRole('list').filter({ hasText: 'Transição' })
  await expect(history.getByRole('listitem')).toHaveCount(5)
  await expectNoAccessibilityViolations(page)
  await snap(page, '07-workflow-completed')
  await logout(page)
})

test('administrator audits the run, including the refused attempt', async ({ page }) => {
  await login(page, 'admin')
  await page.getByRole('link', { name: 'Auditoria' }).first().click()
  await page.getByLabel('Recurso').fill(workflowUrl.split('/').pop()!)
  await expect(page.getByRole('cell', { name: 'Recusado' })).toHaveCount(1)
  await expect(page.getByRole('cell', { name: 'Sucesso' })).toHaveCount(5)
  await expectNoAccessibilityViolations(page)
  await snap(page, '08-audit')

  await page.getByRole('link', { name: 'Usuários' }).first().click()
  await expect(page.getByRole('cell', { name: /Marcos Gestor/ })).toBeVisible()
  await expectNoAccessibilityViolations(page)
  await snap(page, '09-users')
})

test('regular users do not see administrative areas', async ({ page }) => {
  await login(page, 'user')
  await expect(page.getByRole('link', { name: 'Auditoria' })).toHaveCount(0)
  await page.goto('/users')
  await expect(page.getByRole('heading', { name: 'Acesso restrito' })).toBeVisible()
})

test('layout works on a 360px phone and in dark mode', async ({ browser }) => {
  const context = await browser.newContext({ viewport: { width: 360, height: 780 }, colorScheme: 'dark', locale: 'pt-BR' })
  const page = await context.newPage()
  await login(page, 'admin')
  await page.goto(workflowUrl)
  // Nenhum bloco do conteúdo pode passar da largura da tela; conteúdo largo (diagrama, tabelas) rola por dentro.
  const widest = await page.evaluate(() =>
    Math.max(...[...document.querySelectorAll('main section')].map((el) => el.getBoundingClientRect().right)),
  )
  expect(widest).toBeLessThanOrEqual(360)
  await page.getByRole('button', { name: 'Abrir menu' }).click()
  await expect(page.getByRole('navigation', { name: 'Principal' })).toBeVisible()
  await expectNoAccessibilityViolations(page)
  await snap(page, '10-mobile-dark')
  await context.close()
})
